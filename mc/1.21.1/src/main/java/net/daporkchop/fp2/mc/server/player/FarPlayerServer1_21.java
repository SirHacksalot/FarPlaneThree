package net.daporkchop.fp2.mc.server.player;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.flow.FlowControl;
import net.daporkchop.fp2.core.server.player.AbstractFarPlayerServer;
import net.daporkchop.fp2.mc.network.FP2Network1_21;
import net.daporkchop.lib.math.vector.Vec3d;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Getter
public class FarPlayerServer1_21 extends AbstractFarPlayerServer {
    @NonNull
    protected final FP2Core fp2;
    @NonNull
    protected final ServerPlayer player;

    @Override
    public Vec3d fp2_IFarPlayer_position() {
        return Vec3d.of(this.player.getX(), this.player.getY(), this.player.getZ());
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet) {
        if (!this.closed) {
            FP2Network1_21.sendToPlayer(packet, this.player);
        }
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet, Consumer<Throwable> handler) {
        if (!this.closed) {
            FP2Network1_21.sendToPlayer(packet, this.player, handler);
        }
    }

    @Override
    public FlowControl fp2_IFarPlayer_flowControl() {
        return FlowControl.none();
    }
}
//?}
