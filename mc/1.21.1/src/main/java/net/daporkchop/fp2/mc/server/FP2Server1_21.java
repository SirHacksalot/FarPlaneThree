package net.daporkchop.fp2.mc.server;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.server.FP2Server;

@Getter
public class FP2Server1_21 extends FP2Server {
    private final FP2Core fp2;

    public FP2Server1_21(@NonNull FP2Core fp2) {
        this.fp2 = fp2;
    }
}
