package net.daporkchop.fp2.mc.client.player;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.FWorldClient;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.player.AbstractFarPlayerClient;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.mc.network.FP2Network1_21;

@Getter
public class FarPlayerClient1_21 extends AbstractFarPlayerClient<FP2Core> {
    private final FP2Core fp2;

    public FarPlayerClient1_21(@NonNull FP2Core fp2) {
        this.fp2 = fp2;
        this.ready();
    }

    @Override
    public FWorldClient world() {
        return null;
    }

    @Override
    protected IFarLevelClient loadActiveLevel() {
        throw new UnsupportedOperationException("world/level abstractions not yet implemented");
    }

    @Override
    public void send(@NonNull IPacket packet) {
        FP2Network1_21.sendToServer(packet);
    }
}
//?}
