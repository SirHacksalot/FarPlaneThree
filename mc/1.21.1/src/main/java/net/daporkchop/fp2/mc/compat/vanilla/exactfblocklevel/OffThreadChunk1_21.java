package net.daporkchop.fp2.mc.compat.vanilla.exactfblocklevel;

//? if neoforge {
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.server.level.ServerLevel;

@Getter
public class OffThreadChunk1_21 {
    public static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC =
            PalettedContainer.codecRW(
                    Block.BLOCK_STATE_REGISTRY,
                    BlockState.CODEC,
                    PalettedContainer.Strategy.SECTION_STATES,
                    Blocks.AIR.defaultBlockState());

    public static Codec<PalettedContainerRO<Holder<Biome>>> createBiomeCodec(@NonNull Registry<Biome> biomeRegistry) {
        return PalettedContainer.codecRO(
                biomeRegistry.asHolderIdMap(),
                biomeRegistry.holderByNameCodec(),
                PalettedContainer.Strategy.SECTION_BIOMES,
                biomeRegistry.getHolderOrThrow(Biomes.PLAINS));
    }

    private final int x;
    private final int z;
    private final int minSection;

    private final LevelChunkSection[] sections;
    private final DataLayer[] blockLight;
    private final DataLayer[] skyLight; // null if dimension has no sky light

    public OffThreadChunk1_21(@NonNull ServerLevel serverLevel,
                               @NonNull Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec,
                               @NonNull CompoundTag rootTag) {
        this.x = rootTag.getInt(ChunkSerializer.X_POS_TAG);
        this.z = rootTag.getInt(ChunkSerializer.Z_POS_TAG);
        this.minSection = serverLevel.getMinSection();

        int sectionCount = serverLevel.getSectionsCount();
        this.sections = new LevelChunkSection[sectionCount];
        this.blockLight = new DataLayer[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            this.blockLight[i] = new DataLayer();
        }

        boolean hasSkyLight = serverLevel.dimensionType().hasSkyLight();
        if (hasSkyLight) {
            this.skyLight = new DataLayer[sectionCount];
            for (int i = 0; i < sectionCount; i++) {
                this.skyLight[i] = new DataLayer();
            }
        } else {
            this.skyLight = null;
        }

        ListTag sectionList = rootTag.getList(ChunkSerializer.SECTIONS_TAG, 10);
        for (int i = 0; i < sectionList.size(); i++) {
            CompoundTag sectionTag = sectionList.getCompound(i);
            int sectionY = sectionTag.getByte("Y");
            int idx = sectionY - this.minSection;
            if (idx < 0 || idx >= sectionCount) continue;

            if (sectionTag.contains("block_states") && sectionTag.contains("biomes")) {
                PalettedContainer<BlockState> states = BLOCK_STATE_CODEC
                        .parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states"))
                        .getOrThrow();
                PalettedContainerRO<Holder<Biome>> biomes = biomeCodec
                        .parse(NbtOps.INSTANCE, sectionTag.getCompound("biomes"))
                        .getOrThrow();
                LevelChunkSection section = new LevelChunkSection(states, biomes);
                section.recalcBlockCounts();
                this.sections[idx] = section;
            }

            if (sectionTag.contains(ChunkSerializer.BLOCK_LIGHT_TAG, 7)) {
                this.blockLight[idx] = new DataLayer(sectionTag.getByteArray(ChunkSerializer.BLOCK_LIGHT_TAG));
            }
            if (this.skyLight != null && sectionTag.contains(ChunkSerializer.SKY_LIGHT_TAG, 7)) {
                this.skyLight[idx] = new DataLayer(sectionTag.getByteArray(ChunkSerializer.SKY_LIGHT_TAG));
            }
        }
    }

    public BlockState getBlockState(int x, int y, int z) {
        int idx = (y >> 4) - this.minSection;
        if (idx >= 0 && idx < this.sections.length) {
            LevelChunkSection section = this.sections[idx];
            if (section != null) {
                return section.getBlockState(x & 15, y & 15, z & 15);
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    public Holder<Biome> getBiome(int x, int y, int z) {
        int idx = (y >> 4) - this.minSection;
        if (idx >= 0 && idx < this.sections.length) {
            LevelChunkSection section = this.sections[idx];
            if (section != null) {
                return section.getNoiseBiome((x & 15) >> 2, (y & 15) >> 2, (z & 15) >> 2);
            }
        }
        return null;
    }

    public int getBlockLight(int x, int y, int z) {
        int idx = (y >> 4) - this.minSection;
        if (idx >= 0 && idx < this.blockLight.length) {
            return this.blockLight[idx].get(x & 15, y & 15, z & 15);
        }
        return 0;
    }

    public int getSkyLight(int x, int y, int z) {
        if (this.skyLight == null) return 0;
        int idx = (y >> 4) - this.minSection;
        if (idx >= 0 && idx < this.skyLight.length) {
            return this.skyLight[idx].get(x & 15, y & 15, z & 15);
        }
        return 0;
    }
}
//?}
