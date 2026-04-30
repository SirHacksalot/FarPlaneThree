package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.minecraft.core.Direction;

import java.util.stream.IntStream;
import java.util.stream.Stream;

// MVP stub: never updates its flag bitmap, so renderingBlocked() always returns false.
// Result: fp2 terrain always renders, even where vanilla terrain would normally cover it.
// Replace with a proper bridge from net.minecraft.client.renderer.LevelRenderer's
// SectionRenderDispatcher state when occlusion correctness is needed.
public class TerrainRenderingBlockedTracker1_21 extends TerrainRenderingBlockedTracker {
    public TerrainRenderingBlockedTracker1_21(FP2Client client) {
        super(client);
    }

    @Override
    protected int[] getExpectedFaceOffsets() {
        return Stream.of(Direction.values())
                .flatMapToInt(direction -> IntStream.of(direction.getStepX(), direction.getStepY(), direction.getStepZ()))
                .toArray();
    }
}
//?}
