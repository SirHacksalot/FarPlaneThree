package net.daporkchop.fp2.mc.compat.vanilla.exactfblocklevel;

//? if neoforge {
import lombok.NonNull;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.GenerationNotAllowedException;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractChunksExactFBlockLevelHolder;
import net.daporkchop.fp2.core.minecraft.world.chunks.AbstractPrefetchedChunksExactFBlockLevel;
import net.daporkchop.lib.common.math.BinMath;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public final class PrefetchedChunksFBlockLevel1_21 extends AbstractPrefetchedChunksExactFBlockLevel<OffThreadChunk1_21> {
    public PrefetchedChunksFBlockLevel1_21(@NonNull AbstractChunksExactFBlockLevelHolder<OffThreadChunk1_21> holder,
                                            boolean generationAllowed,
                                            @NonNull List<OffThreadChunk1_21> chunks) {
        super(holder, generationAllowed, chunks);
    }

    @Override
    protected long packedChunkPosition(@NonNull OffThreadChunk1_21 chunk) {
        return BinMath.packXY(chunk.x(), chunk.z());
    }

    @Override
    protected int getState(int x, int y, int z, OffThreadChunk1_21 chunk) throws GenerationNotAllowedException {
        BlockState blockState = chunk.getBlockState(x, y, z);
        FluidState fluidState = blockState.getFluidState();
        if (!fluidState.isEmpty()) {
            blockState = fluidState.createLegacyBlock();
        }
        return this.registry().state2id(blockState);
    }

    @Override
    protected int getBiome(int x, int y, int z, OffThreadChunk1_21 chunk) throws GenerationNotAllowedException {
        Holder<Biome> holder = chunk.getBiome(x, y, z);
        return holder != null ? this.registry().biome2id(holder) : 0;
    }

    @Override
    protected byte getLight(int x, int y, int z, OffThreadChunk1_21 chunk) throws GenerationNotAllowedException {
        return BlockLevelConstants.packLight(chunk.getSkyLight(x, y, z), chunk.getBlockLight(x, y, z));
    }
}
//?}
