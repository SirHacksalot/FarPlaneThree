package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.daporkchop.fp2.gl.OpenGL;
import net.daporkchop.fp2.mc.client.world.level.FLevelClient1_21;
import net.daporkchop.fp2.mc.world.registry.GameRegistry1_21;
import net.daporkchop.lib.common.misc.threadlocal.TL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class LevelRenderer1_21 implements LevelRenderer, AutoCloseable {
    private static final TL<SingleBiomeBlockAccess1_21> SINGLE_BIOME_BLOCK_ACCESS_CACHE = TL.initializedWith(SingleBiomeBlockAccess1_21::new);
    private static final TL<BlockPos.MutableBlockPos> MUTABLE_POS_CACHE = TL.initializedWith(BlockPos.MutableBlockPos::new);

    protected final FP2Core fp2;
    protected final Minecraft mc;
    protected final FLevelClient1_21 level;

    protected final TextureUVs1_21 textureUVs;
    protected final TerrainRenderingBlockedTracker1_21 blockedTracker;

    protected final GameRegistry1_21 registry;
    // 0 = SOLID, 1 = CUTOUT, 2 = TRANSLUCENT.
    protected final byte[] renderTypeLookup;

    public LevelRenderer1_21(@NonNull Minecraft mc, @NonNull FLevelClient1_21 level) {
        this.fp2 = level.fp2();
        this.mc = mc;
        this.level = level;
        this.registry = (GameRegistry1_21) level.registry();

        this.renderTypeLookup = new byte[this.registry.statesCount()];
        RenderType solid = RenderType.solid();
        RenderType cutout = RenderType.cutout();
        RenderType cutoutMipped = RenderType.cutoutMipped();
        RenderType translucent = RenderType.translucent();
        RenderType tripwire = RenderType.tripwire();
        this.registry.states().forEach(stateId -> {
            BlockState state = this.registry.id2state(stateId);
            RenderType type = ItemBlockRenderTypes.getChunkRenderType(state);
            byte typeIndex;
            if (type == solid) {
                typeIndex = 0;
            } else if (type == cutout || type == cutoutMipped) {
                typeIndex = 1;
            } else if (type == translucent || type == tripwire) {
                typeIndex = 2;
            } else {
                typeIndex = 0; // unknown render types fall back to solid; safer than throwing mid-iteration
            }
            this.renderTypeLookup[stateId] = typeIndex;
        });

        this.textureUVs = new TextureUVs1_21(this.fp2, level.registry(), mc);
        this.blockedTracker = new TerrainRenderingBlockedTracker1_21(this.fp2.client());
    }

    @Override
    public int renderTypeForState(int state) {
        return this.renderTypeLookup[state];
    }

    @Override
    @SuppressWarnings("unchecked")
    public int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z) {
        SingleBiomeBlockAccess1_21 access = SINGLE_BIOME_BLOCK_ACCESS_CACHE.get();
        access.biome((Holder<Biome>) this.registry.id2biome(biome));

        BlockPos.MutableBlockPos pos = MUTABLE_POS_CACHE.get();
        pos.set(x, y, z);

        return this.mc.getBlockColors().getColor(this.registry.id2state(state), access, pos, 0);
    }

    @Override
    public TerrainRenderingBlockedTracker blockedTracker() {
        return this.blockedTracker;
    }

    @Override
    public TextureUVs textureUVs() {
        return this.textureUVs;
    }

    @Override
    public OpenGL gl() {
        return this.fp2.client().gl();
    }

    @Override
    public void close() {
        this.blockedTracker.close();
        // textureUVs is an AbstractReleasable; release via doRelease through close-equivalent.
        // AbstractTextureUVs doesn't expose a public close method, so rely on FP2 lifecycle.
    }
}
//?}
