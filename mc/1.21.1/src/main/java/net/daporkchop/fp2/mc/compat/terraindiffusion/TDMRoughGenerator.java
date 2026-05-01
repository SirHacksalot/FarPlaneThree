package net.daporkchop.fp2.mc.compat.terraindiffusion;

//? if neoforge {
import com.github.xandergos.terraindiffusionmc.config.TerrainDiffusionConfig;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider;
import com.github.xandergos.terraindiffusionmc.pipeline.LocalTerrainProvider.HeightmapData;
import com.github.xandergos.terraindiffusionmc.world.HeightConverter;
import com.github.xandergos.terraindiffusionmc.world.TerrainDiffusionBiomeSource;
import com.github.xandergos.terraindiffusionmc.world.WorldScaleManager;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.engine.Tile;
import net.daporkchop.fp2.core.engine.TileData;
import net.daporkchop.fp2.core.engine.TilePos;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.engine.server.gen.rough.AbstractDualContouringRoughVoxelGenerator;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.lib.common.reference.ReferenceStrength;
import net.daporkchop.lib.common.reference.cache.Cached;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Rough generator that drives FP2's far-plane LOD mesh from terrain-diffusion-mc's neural heightmap.
 *
 * <p>Per-tile workflow: sample TDM's {@link LocalTerrainProvider} once per (x, z) cache column,
 * convert raw meters → MC block height with the same {@link HeightConverter} that the exact
 * density function uses, resolve each column's biome via {@link TerrainDiffusionBiomeSource}
 * (its internal heightmap fetch hits the same cache we just populated), then fill water + stone
 * density layers for dual-contouring.
 *
 * <p>Surface voxels (the topmost solid voxel in each column) get a biome-flavored top block:
 * snow for cold biomes, sand for desert/badlands/beach, grass otherwise. Sub-surface stays stone
 * because LOD voxels span 1–128 blocks vertically depending on level — a separate dirt band
 * doesn't show up clearly past level 1 and isn't worth the extra logic. Grass is biome-tinted
 * by the renderer using {@code data.biome}.
 */
public final class TDMRoughGenerator extends AbstractDualContouringRoughVoxelGenerator<TDMRoughGenerator.TileContext> {
    private static final int TDM_TILE_SIZE = TerrainDiffusionConfig.tileSize();
    private static final int TDM_TILE_SHIFT = Integer.numberOfTrailingZeros(TDM_TILE_SIZE);

    // Caps how many FP2 worker threads can be enqueued in TDM's single-thread inference
    // executor at once. Without this, FP2's 12 worker threads spam the executor with LOD
    // requests, starving MC's chunk generator (which uses the same executor via the density
    // function and biome source) and producing very slow vanilla-chunk loading. 2 lets some
    // FP2 throughput through while leaving room for MC's per-chunk requests to interleave.
    private static final Semaphore IN_FLIGHT = new Semaphore(2, true);

    // Surface category encoding stored in TileContext.surfaceCategory.
    private static final byte CAT_GRASS = 0;
    private static final byte CAT_SAND  = 1;
    private static final byte CAT_SNOW  = 2;
    private static final byte CAT_STONE = 3;

    private final TerrainDiffusionBiomeSource tdmBiomeSource;
    private final long worldSeed;

    private final int stoneStateId;
    private final int waterStateId;
    private final int grassStateId;
    private final int sandStateId;
    private final int snowBlockStateId;
    private final int oakLogStateId;
    private final int oakLeavesStateId;

    private final Cached<TileContext> tileContextCache = Cached.threadLocal(TileContext::new, ReferenceStrength.WEAK);

    // Memoizes (TDM biome short → FP2 biome id). Without this, every cache column would call
    // tdmBiomeSource.getNoiseBiome — which internally re-runs fetchHeightmap on the same TDM
    // tile we already read this iteration, and does a HashMap lookup. After the first ~80
    // unique biomes have been seen, this map is fully warm and lookups are O(1).
    private final ConcurrentHashMap<Short, Integer> tdmShortToFp2BiomeId = new ConcurrentHashMap<>();

    public TDMRoughGenerator(@NonNull IFarLevelServer world, @NonNull IFarTileProvider provider,
                             @NonNull TerrainDiffusionBiomeSource tdmBiomeSource, long worldSeed) {
        super(world, provider);
        this.tdmBiomeSource = tdmBiomeSource;
        this.worldSeed = worldSeed;
        FGameRegistry reg = this.registry();
        this.stoneStateId      = reg.state2id(Blocks.STONE.defaultBlockState());
        this.waterStateId      = reg.state2id(Blocks.WATER.defaultBlockState());
        this.grassStateId      = reg.state2id(Blocks.GRASS_BLOCK.defaultBlockState());
        this.sandStateId       = reg.state2id(Blocks.SAND.defaultBlockState());
        this.snowBlockStateId  = reg.state2id(Blocks.SNOW_BLOCK.defaultBlockState());
        this.oakLogStateId     = reg.state2id(Blocks.OAK_LOG.defaultBlockState());
        this.oakLeavesStateId  = reg.state2id(Blocks.OAK_LEAVES.defaultBlockState());
    }

    @Override
    public boolean canGenerate(@NonNull TilePos pos) {
        return true;
    }

    @Override
    public void generate(@NonNull TilePos pos, @NonNull Tile tile) {
        try {
            IN_FLIGHT.acquire();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return;
        }
        try {
            this.generate0(pos, tile);
        } finally {
            IN_FLIGHT.release();
        }
    }

    private void generate0(TilePos pos, Tile tile) {
        int level = pos.level();
        int baseX = pos.minBlockX();
        int baseY = pos.minBlockY();
        int baseZ = pos.minBlockZ();

        // Read scale fresh per tile: WorldScaleManager.initializeForWorld() races with FP2's
        // CreationEvent at world load. Caching in the constructor risks capturing DEFAULT_SCALE
        // before TDM's per-world load callback applies the saved value.
        int worldScale = WorldScaleManager.getCurrentScale();
        TileContext ctx = this.tileContextCache.get();
        ctx.level = level;
        ctx.baseX = baseX;
        ctx.baseZ = baseZ;
        LocalTerrainProvider tdm = LocalTerrainProvider.getInstance();
        FGameRegistry reg = this.registry();

        // Sample TDM heightmap and resolve per-column biome at every (x, z) cache column.
        // Each fetchHeightmap hits TDM's per-tile cache after the first lookup in a new TDM
        // tile, and tdmBiomeSource.getNoiseBiome internally hits the same cache.
        for (int x = CACHE_MIN; x < CACHE_MAX; x++) {
            int worldX = baseX + (x << level);
            int quartX = QuartPos.fromBlock(worldX);
            for (int z = CACHE_MIN; z < CACHE_MAX; z++) {
                int worldZ = baseZ + (z << level);
                int tileStartX = (worldX >> TDM_TILE_SHIFT) << TDM_TILE_SHIFT;
                int tileStartZ = (worldZ >> TDM_TILE_SHIFT) << TDM_TILE_SHIFT;

                // TDM's coordinate convention: i = Z, j = X.
                HeightmapData data = tdm.fetchHeightmap(
                        tileStartZ, tileStartX,
                        tileStartZ + TDM_TILE_SIZE, tileStartX + TDM_TILE_SIZE);

                int localX = worldX - tileStartX;
                int localZ = worldZ - tileStartZ;
                int colIdx = (x - CACHE_MIN) * CACHE_SIZE + (z - CACHE_MIN);

                if (data != null && data.heightmap != null) {
                    short rawMeters = data.heightmap[localZ][localX];
                    int mcHeight = HeightConverter.convertToMinecraftHeight(rawMeters, worldScale);
                    ctx.mcHeights[colIdx] = mcHeight;

                    short tdmBiomeShort = data.biomeIds != null ? data.biomeIds[localZ][localX] : (short) 0;
                    byte category = surfaceCategoryFromTdmBiome(tdmBiomeShort);
                    ctx.surfaceCategory[colIdx] = category;
                    ctx.fp2BiomeIds[colIdx] = this.resolveFp2BiomeId(tdmBiomeShort, quartX, worldZ, reg);
                    ctx.treeHeights[colIdx] = treeHeightAt(worldX, worldZ, mcHeight, category, level, this.worldSeed);
                } else {
                    // No data — push the height below the world so dual-contour produces no mesh here.
                    ctx.mcHeights[colIdx] = Integer.MIN_VALUE / 2;
                    ctx.surfaceCategory[colIdx] = CAT_STONE;
                    ctx.fp2BiomeIds[colIdx] = 0;
                    ctx.treeHeights[colIdx] = 0;
                }
            }
        }

        double[][] densityMap = this.densityMapCache.get();
        double scaleFactor = 1.0d / (1 << level);
        int seaLevel = this.seaLevel();

        // Layer 0 — water. Identical to CWG: positive below sea, scale-corrected.
        // Z is innermost in cacheIndex, so each Arrays.fill writes one z-row at fixed (x, y).
        assert cacheIndex(0, 0, 1) - cacheIndex(0, 0, 0) == 1 : "cache coordinate order must be z-minor";
        for (int x = CACHE_MIN; x < CACHE_MAX; x++) {
            for (int y = CACHE_MIN; y < CACHE_MAX; y++) {
                int idx = cacheIndex(x, y, CACHE_MIN);
                Arrays.fill(densityMap[0], idx, idx + CACHE_SIZE,
                        ((seaLevel - 0.125d) - (baseY + (y << level))) * scaleFactor);
            }
        }

        // Layer 1 — stone (+ trees). effectiveHeight extends mcHeight by treeHeight at columns
        // we marked as having a tree, so dual-contour produces mesh up to the tree top. Block
        // type (log vs leaves vs grass vs stone) is decided per voxel in getFaceState.
        for (int x = CACHE_MIN; x < CACHE_MAX; x++) {
            for (int z = CACHE_MIN; z < CACHE_MAX; z++) {
                int colIdx = (x - CACHE_MIN) * CACHE_SIZE + (z - CACHE_MIN);
                double effectiveHeight = ctx.mcHeights[colIdx] + ctx.treeHeights[colIdx];
                for (int y = CACHE_MIN; y < CACHE_MAX; y++) {
                    int worldY = baseY + (y << level);
                    densityMap[1][cacheIndex(x, y, z)] = (effectiveHeight - worldY) * scaleFactor;
                }
            }
        }

        this.dualContour(baseX, baseY, baseZ, level, tile, densityMap, ctx);
    }

    @Override
    protected int getFaceState(int blockX, int blockY, int blockZ, int level,
                               double nx, double ny, double nz,
                               double density0, double density1, int edge, int layer, TileContext ctx) {
        if (layer == 0) {
            return this.waterStateId;
        }

        int colIdx = colIdxFromWorld(blockX, blockZ, ctx);
        int mcHeight = ctx.mcHeights[colIdx];
        int treeHeight = ctx.treeHeights[colIdx];
        int voxelTopY = blockY + (1 << level);

        // Tree (only set at low levels where individual blocks are visible). The voxel that
        // straddles the tree's top → leaves; voxels strictly above the ground but at or below
        // the tree's top → trunk. Both decisions short-circuit before the ground check below
        // so a tree-bearing column shows leaves at its top instead of grass.
        if (treeHeight > 0) {
            int treeTop = mcHeight + treeHeight;
            if (blockY <= treeTop && voxelTopY > treeTop) {
                return this.oakLeavesStateId;
            }
            if (blockY > mcHeight && voxelTopY <= treeTop) {
                return this.oakLogStateId;
            }
        }

        // Ground surface: voxel straddles mcHeight. At level 0 this is exact; at higher levels
        // each voxel spans (1 << level) blocks vertically.
        if (blockY <= mcHeight && voxelTopY > mcHeight) {
            return switch (ctx.surfaceCategory[colIdx]) {
                case CAT_SAND  -> this.sandStateId;
                case CAT_SNOW  -> this.snowBlockStateId;
                case CAT_STONE -> this.stoneStateId;
                default        -> this.grassStateId;
            };
        }

        return this.stoneStateId;
    }

    @Override
    protected void populateVoxelBlockData(int blockX, int blockY, int blockZ, int level,
                                          double nx, double ny, double nz, TileData data, TileContext ctx) {
        int colIdx = colIdxFromWorld(blockX, blockZ, ctx);
        data.biome = ctx.fp2BiomeIds[colIdx];

        blockY++;
        int seaLevelLod = this.seaLevel() >> level << level;
        data.light = BlockLevelConstants.packLight(
                blockY < seaLevelLod ? Math.max(15 - (seaLevelLod - blockY) * 3, 0) : 15, 0);
    }

    private static final int FOREST_CANOPY_HEIGHT = 5;

    /**
     * Per-column "tree" thickness for the forest-carpet LOD model. Every grass column gets a
     * uniform ~5-block extension above the heightmap; the top voxel renders as leaves and the
     * voxels in between as log. From the LOD ring this reads as a continuous forest canopy,
     * which matches what a real MC forest actually looks like from distance — individual tree
     * silhouettes blur together, the dominant visual is the leaf surface.
     *
     * <p>Per-tree placement was tried first and produced 1-block-wide stick "trees" that looked
     * obviously fake next to exact-gen 5×5-canopy oaks. The voxel format has only two density
     * layers (water + solid), so a real canopy with empty space underneath isn't expressible —
     * any solid leaf voxel forces solid below it. The carpet sidesteps that by being honest:
     * it's a flat-topped forest, not a sparse one. Restricted to level 0 because at coarser LOD
     * the canopy thickness is smaller than the voxel and disappears anyway.
     */
    private static int treeHeightAt(int worldX, int worldZ, int mcHeight, byte category, int level, long seed) {
        if (level != 0 || category != CAT_GRASS) {
            return 0;
        }
        return FOREST_CANOPY_HEIGHT;
    }

    /**
     * Caches (TDM short → FP2 biome id). Cold lookups invoke {@link TerrainDiffusionBiomeSource#getNoiseBiome},
     * which does an internal {@code fetchHeightmap} + map lookup. Warm lookups skip both.
     */
    private int resolveFp2BiomeId(short tdmBiomeShort, int quartX, int worldZ, FGameRegistry reg) {
        Integer cached = this.tdmShortToFp2BiomeId.get(tdmBiomeShort);
        if (cached != null) {
            return cached;
        }
        // Sampler arg is unused by TDM's getNoiseBiome impl. Quart-coords required.
        Holder<Biome> biomeHolder = this.tdmBiomeSource.getNoiseBiome(quartX, 0, QuartPos.fromBlock(worldZ), null);
        int fp2Id = reg.biome2id(biomeHolder);
        this.tdmShortToFp2BiomeId.putIfAbsent(tdmBiomeShort, fp2Id);
        return fp2Id;
    }

    private static int colIdxFromWorld(int blockX, int blockZ, TileContext ctx) {
        int dx = (blockX - ctx.baseX) >> ctx.level;
        int dz = (blockZ - ctx.baseZ) >> ctx.level;
        return (dx - CACHE_MIN) * CACHE_SIZE + (dz - CACHE_MIN);
    }

    /**
     * TDM biome short → surface block category. IDs reference TerrainDiffusionBiomeSource's
     * hardcoded mapping. Biomes not listed default to CAT_GRASS — the renderer biome-tints
     * grass so the LOD picks up a reasonable color even for unmapped biomes.
     */
    private static byte surfaceCategoryFromTdmBiome(short tdmBiomeShort) {
        switch (tdmBiomeShort) {
            // Snowy biomes (vanilla)
            case 3, 16, 32, 33, 48 -> { return CAT_SNOW; }
            // Desert / badlands / dry biomes (vanilla)
            case 5, 26, 17 -> { return CAT_SAND; }
            // Stony peaks (vanilla)
            case 35 -> { return CAT_STONE; }
            // Beach-like ocean shore is sand
            case 41 -> { return CAT_SAND; }
            // BWG snowy
            case 226, 227, 246, 214 -> { return CAT_SNOW; }
            // BWG desert / badlands
            case 200, 201, 204, 233, 237, 240, 244, 247, 253 -> { return CAT_SAND; }
            // BWG stony peaks
            case 217, 228, 241 -> { return CAT_STONE; }
            // BWG dacite shore
            case 218 -> { return CAT_SAND; }
            default -> { return CAT_GRASS; }
        }
    }

    static final class TileContext {
        int baseX, baseZ, level;
        final int[] mcHeights        = new int[CACHE_SIZE * CACHE_SIZE];
        final byte[] surfaceCategory = new byte[CACHE_SIZE * CACHE_SIZE];
        final int[] fp2BiomeIds      = new int[CACHE_SIZE * CACHE_SIZE];
        final int[] treeHeights      = new int[CACHE_SIZE * CACHE_SIZE];
    }
}
//?}
