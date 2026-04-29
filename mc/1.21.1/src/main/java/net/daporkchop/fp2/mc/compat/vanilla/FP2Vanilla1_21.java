package net.daporkchop.fp2.mc.compat.vanilla;

//? if neoforge {
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.minecraft.server.level.ServerLevel;

public class FP2Vanilla1_21 {
    private static final int HORIZONTAL_LIMIT = 29_999_984;

    @FEventHandler(name = "vanilla_world_coordinate_limits")
    public IntAxisAlignedBB getCoordinateLimits(GetCoordinateLimitsEvent event) {
        ServerLevel level = (ServerLevel) event.world().implLevel();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        return new IntAxisAlignedBB(-HORIZONTAL_LIMIT, minY, -HORIZONTAL_LIMIT,
                HORIZONTAL_LIMIT + 1, maxY, HORIZONTAL_LIMIT + 1);
    }
}
//?}
