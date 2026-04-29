package net.daporkchop.fp2.mc.server.world;

//? if neoforge {
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.mc.server.world.level.FLevelServer1_21;
import net.minecraft.server.level.ServerLevel;

import static net.daporkchop.fp2.api.FP2.*;

@RequiredArgsConstructor
public class TerrainGeneratorInfo1_21 implements TerrainGeneratorInfo {
    @NonNull
    protected final FLevelServer1_21 level;
    @NonNull
    protected final ServerLevel serverLevel;

    @Override
    public FLevelServer1_21 world() {
        return this.level;
    }

    @Override
    public Object implGenerator() {
        return fp2().eventBus().fireAndGetFirst(new GetTerrainGeneratorEvent(this.world())).get();
    }

    @Override
    public String generator() {
        return this.serverLevel.dimension().location().toString();
    }

    @Override
    public String options() {
        return "";
    }

    @Override
    public long seed() {
        return this.serverLevel.getSeed();
    }
}
//?}
