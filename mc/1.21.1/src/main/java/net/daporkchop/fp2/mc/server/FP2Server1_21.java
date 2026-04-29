package net.daporkchop.fp2.mc.server;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.server.FP2Server;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;

//? if neoforge {
import net.daporkchop.fp2.mc.server.player.FarPlayerServer1_21;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
//?}

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FP2Server1_21 extends FP2Server {
    private final FP2Core fp2;
    private final Map<UUID, IFarPlayerServer> players = new ConcurrentHashMap<>();

    public FP2Server1_21(@NonNull FP2Core fp2) {
        this.fp2 = fp2;
    }

    @Override
    public void init(@NonNull FutureExecutor serverThreadExecutor) {
        super.init(serverThreadExecutor);
        //? if neoforge {
        NeoForge.EVENT_BUS.register(this);
        //?}
    }

    public void handleIncoming(@NonNull UUID playerId, @NonNull Object packet) {
        IFarPlayerServer player = this.players.get(playerId);
        if (player != null) {
            player.fp2_IFarPlayerServer_handle(packet);
        }
    }

    //? if neoforge {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        FarPlayerServer1_21 fp2Player = new FarPlayerServer1_21(this.fp2, serverPlayer);
        this.players.put(serverPlayer.getUUID(), fp2Player);
        fp2Player.fp2_IFarPlayer_serverConfig(this.fp2.globalConfig());
        fp2Player.fp2_IFarPlayer_sendPacket(SPacketHandshake.create());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        IFarPlayerServer fp2Player = this.players.remove(serverPlayer.getUUID());
        if (fp2Player != null) {
            fp2Player.fp2_IFarPlayer_close();
        }
    }
    //?}
}
