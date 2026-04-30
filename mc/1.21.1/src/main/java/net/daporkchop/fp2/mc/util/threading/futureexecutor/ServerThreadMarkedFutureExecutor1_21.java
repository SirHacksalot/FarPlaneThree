package net.daporkchop.fp2.mc.util.threading.futureexecutor;

//? if neoforge {
import lombok.NonNull;
import net.daporkchop.fp2.core.util.threading.futureexecutor.AbstractMarkedFutureExecutor;
import net.minecraft.server.MinecraftServer;

public class ServerThreadMarkedFutureExecutor1_21 extends AbstractMarkedFutureExecutor {
    // Time budget (nanoseconds) for processing FP2 server-side tasks per server tick. At
    // 20 TPS the server has 50ms total per tick; capping FP2 at 8ms leaves 42ms for vanilla
    // (mob ticks, chunk loads, the unload/save cascade triggered by FP2's tile-gen chunk
    // reads, etc.). Without this cap, doAllWork() drains the entire backlog every tick —
    // under heavy tile generation that backlog can be tens of thousands of tasks, producing
    // multi-second tick spikes (profiler observed 5.7s/tick @ 41k queued tasks).
    private static final long TICK_BUDGET_NANOS = 8_000_000L;

    public ServerThreadMarkedFutureExecutor1_21(@NonNull MinecraftServer server) {
        super(server.getRunningThread());
        this.start();
    }

    public void tick() {
        long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
        while (System.nanoTime() < deadline && this.doWork()) {
            //doWork() returns true when there is more work; loop until either the queue is
            //empty or our time budget is exhausted. Backlog will drain over subsequent ticks.
        }
    }
}
//?}
