package net.daporkchop.fp2.mc.server.world;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.server.world.AbstractWorldServer;
import net.daporkchop.fp2.mc.server.world.level.FLevelServer1_21;
import net.daporkchop.fp2.mc.util.threading.futureexecutor.ServerThreadMarkedFutureExecutor1_21;
import net.daporkchop.fp2.mc.world.registry.GameRegistry1_21;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FWorldServer1_21 extends AbstractWorldServer<FP2Core, MinecraftServer, FWorldServer1_21, ServerLevel, FLevelServer1_21> {
    private final GameRegistry1_21 gameRegistry;
    private final ServerThreadMarkedFutureExecutor1_21 serverExecutor;
    private final Map<ResourceKey<Level>, FLevelServer1_21> levelsByKey = new ConcurrentHashMap<>();

    public FWorldServer1_21(@NonNull FP2Core fp2, @NonNull MinecraftServer implWorld) {
        super(fp2, implWorld, implWorld.getWorldPath(LevelResource.ROOT));
        this.gameRegistry = new GameRegistry1_21(implWorld.registryAccess());
        this.serverExecutor = new ServerThreadMarkedFutureExecutor1_21(implWorld);
    }

    @Override
    protected FLevelServer1_21 createLevel(@NonNull Identifier id, @NonNull ServerLevel implLevel) {
        FLevelServer1_21 level = new FLevelServer1_21(this.fp2(), implLevel, this, id);
        this.levelsByKey.put(implLevel.dimension(), level);
        return level;
    }

    public FLevelServer1_21 getLevel(@NonNull ResourceKey<Level> dimensionKey) {
        return this.levelsByKey.get(dimensionKey);
    }

    @Override
    protected void doClose() throws Exception {
        this.levelsByKey.clear();
        try (AutoCloseable closeSuper = super::doClose;
             ServerThreadMarkedFutureExecutor1_21 exec = this.serverExecutor) {
        }
    }
}
//?}
