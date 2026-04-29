package net.daporkchop.fp2.mc.server.player;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.flow.FlowControl;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.mc.network.FP2Network1_21;
import net.daporkchop.lib.math.vector.Vec3d;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

@Getter
public class FarPlayerServer1_21 implements IFarPlayerServer {
    private final FP2Core fp2;
    private final ServerPlayer player;

    public FarPlayerServer1_21(@NonNull FP2Core fp2, @NonNull ServerPlayer player) {
        this.fp2 = fp2;
        this.player = player;
    }

    @Override
    public Vec3d fp2_IFarPlayer_position() {
        return Vec3d.of(player.getX(), player.getY(), player.getZ());
    }

    @Override
    public void fp2_IFarPlayerServer_handle(@NonNull Object packet) {
        // TODO: dispatch incoming serverbound packets to game logic
    }

    @Override
    public void fp2_IFarPlayer_serverConfig(FP2Config serverConfig) {
        // TODO: send SPacketUpdateConfig to client
    }

    @Override
    public void fp2_IFarPlayer_joinedWorld(@NonNull IFarLevelServer world) {
        // TODO: begin tile streaming for this world
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet) {
        FP2Network1_21.sendToPlayer(packet, this.player);
    }

    @Override
    public void fp2_IFarPlayer_sendPacket(@NonNull IPacket packet, Consumer<Throwable> handler) {
        FP2Network1_21.sendToPlayer(packet, this.player, handler);
    }

    @Override
    public FlowControl fp2_IFarPlayer_flowControl() {
        return FlowControl.none();
    }

    @Override
    public void fp2_IFarPlayer_update() {
        // TODO: periodic per-tick update
    }

    @Override
    public void fp2_IFarPlayer_close() {
        // TODO: clean up tile streaming resources
    }
}
//?}
