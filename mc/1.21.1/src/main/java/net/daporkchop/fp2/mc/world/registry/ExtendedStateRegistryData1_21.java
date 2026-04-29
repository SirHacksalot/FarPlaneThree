package net.daporkchop.fp2.mc.world.registry;

import lombok.NonNull;
import net.daporkchop.fp2.core.world.registry.AbstractDenseExtendedStateRegistryData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import static net.daporkchop.fp2.api.world.level.BlockLevelConstants.*;

public final class ExtendedStateRegistryData1_21 extends AbstractDenseExtendedStateRegistryData<BlockState> {
    public ExtendedStateRegistryData1_21(@NonNull GameRegistry1_21 registry) {
        super(registry);
    }

    @Override
    protected int type(int id, BlockState state) {
        if (state.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
            return BLOCK_TYPE_OPAQUE;
        } else if (state.isSolid() || !state.getFluidState().isEmpty()) {
            return BLOCK_TYPE_TRANSPARENT;
        } else {
            return BLOCK_TYPE_INVISIBLE;
        }
    }

    @Override
    protected int lightOpacity(int id, BlockState state) {
        return state.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    @Override
    protected int lightEmission(int id, BlockState state) {
        return state.getLightEmission();
    }
}
