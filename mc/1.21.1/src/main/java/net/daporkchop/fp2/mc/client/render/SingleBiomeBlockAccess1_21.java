package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

// Minimal BlockAndTintGetter that resolves to a single biome at every position; used so
// BlockColors.getColor(state, this, pos, tintIndex) can compute the correct tint for grass/
// foliage/water without referencing a real Level. Only getBlockTint is on the hot path —
// the other methods aren't called during tint resolution and throw if invoked.
@Getter
@Setter
public class SingleBiomeBlockAccess1_21 implements BlockAndTintGetter {
    protected Holder<Biome> biome;

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return colorResolver.getColor(this.biome.value(), pos.getX(), pos.getZ());
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return 1.0f;
    }

    @Override
    public net.minecraft.world.level.lighting.LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBrightness(LightLayer lightLayer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getHeight() {
        return 384;
    }

    @Override
    public int getMinBuildHeight() {
        return -64;
    }
}
//?}
