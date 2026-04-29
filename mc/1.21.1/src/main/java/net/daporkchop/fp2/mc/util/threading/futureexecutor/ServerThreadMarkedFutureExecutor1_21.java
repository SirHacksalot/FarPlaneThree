package net.daporkchop.fp2.mc.util.threading.futureexecutor;

//? if neoforge {
import lombok.NonNull;
import net.daporkchop.fp2.core.util.threading.futureexecutor.AbstractMarkedFutureExecutor;
import net.minecraft.server.MinecraftServer;

public class ServerThreadMarkedFutureExecutor1_21 extends AbstractMarkedFutureExecutor {
    public ServerThreadMarkedFutureExecutor1_21(@NonNull MinecraftServer server) {
        super(server.getRunningThread());
        this.start();
    }

    public void tick() {
        this.doAllWork();
    }
}
//?}
