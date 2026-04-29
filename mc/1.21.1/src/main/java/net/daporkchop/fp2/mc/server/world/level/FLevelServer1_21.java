package net.daporkchop.fp2.mc.server.world.level;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.core.server.world.level.AbstractLevelServer;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.mc.server.world.FWorldServer1_21;
import net.daporkchop.fp2.mc.server.world.TerrainGeneratorInfo1_21;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;

@Getter
public class FLevelServer1_21 extends AbstractLevelServer<FP2Core, MinecraftServer, FWorldServer1_21, ServerLevel, FLevelServer1_21> {
    public FLevelServer1_21(@NonNull FP2Core fp2, @NonNull ServerLevel implLevel, @NonNull FWorldServer1_21 world, @NonNull Identifier id) {
        super(fp2, implLevel, world, id, world.gameRegistry());
    }

    @Override
    protected WorkerManager createWorkerManager() {
        return new DefaultWorkerManager(
                this.implLevel().getServer().getRunningThread(),
                this.world().serverExecutor());
    }

    @Override
    public Path levelDirectory() {
        ServerLevel level = this.implLevel();
        return level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .resolve(level.dimension().location().getNamespace())
                .resolve(level.dimension().location().getPath());
    }

    @Override
    public TerrainGeneratorInfo terrainGeneratorInfo() {
        return new TerrainGeneratorInfo1_21(this, this.implLevel());
    }

    @Override
    public int seaLevel() {
        return this.implLevel().getSeaLevel();
    }

    @Override
    public long timestamp() {
        return this.implLevel().getGameTime();
    }
}
//?}
