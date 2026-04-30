package net.daporkchop.fp2.mc.compat.vanilla;

//? if neoforge {
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.core.engine.api.server.IFarTileProvider;
import net.daporkchop.fp2.core.engine.api.server.gen.IFarGeneratorExact;
import net.daporkchop.fp2.core.engine.api.server.gen.IFarGeneratorRough;
import net.daporkchop.fp2.core.engine.server.TileProvider;
import net.daporkchop.fp2.core.engine.server.gen.exact.VanillaVoxelGenerator;
import net.daporkchop.fp2.core.server.event.GetCoordinateLimitsEvent;
import net.daporkchop.fp2.core.server.event.GetExactFBlockLevelEvent;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.mc.compat.vanilla.exactfblocklevel.VanillaExactFBlockLevelHolder1_21;
import net.daporkchop.fp2.mc.server.world.level.FLevelServer1_21;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

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

    @FEventHandler(name = "vanilla_exact_fblock_level")
    public ExactFBlockLevelHolder getExactFBlockLevel(GetExactFBlockLevelEvent event) {
        return new VanillaExactFBlockLevelHolder1_21((FLevelServer1_21) event.level());
    }

    @FEventHandler(name = "vanilla_world_terrain_generator")
    public Object getTerrainGenerator(GetTerrainGeneratorEvent event) {
        return ((ServerLevel) event.world().implLevel()).getChunkSource().getGenerator();
    }

    @FEventHandler(name = "vanilla_generator_exact")
    public IFarGeneratorExact createGeneratorExact(IFarGeneratorExact.CreationEvent event) {
        return new VanillaVoxelGenerator(event.world(), event.provider());
    }

    @FEventHandler(name = "vanilla_generator_rough")
    public Optional<IFarGeneratorRough> createGeneratorRough(IFarGeneratorRough.CreationEvent event) {
        return Optional.empty();
    }

    @FEventHandler(name = "vanilla_tileprovider")
    public IFarTileProvider createTileProvider(IFarTileProvider.CreationEvent event) {
        return new TileProvider.Vanilla(event.world());
    }
}
//?}
