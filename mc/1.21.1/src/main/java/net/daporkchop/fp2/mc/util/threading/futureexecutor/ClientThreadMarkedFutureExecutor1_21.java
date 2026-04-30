package net.daporkchop.fp2.mc.util.threading.futureexecutor;

//? if neoforge {
import net.daporkchop.fp2.core.util.threading.futureexecutor.AbstractMarkedFutureExecutor;

public class ClientThreadMarkedFutureExecutor1_21 extends AbstractMarkedFutureExecutor {
    /** Must be constructed on the client/render thread. */
    public ClientThreadMarkedFutureExecutor1_21() {
        super(Thread.currentThread());
        this.start();
    }

    public Thread thread() {
        return this.thread;
    }

    public void tick() {
        this.doAllWork();
    }
}
//?}
