package net.daporkchop.fp2.mc.server;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.network.packet.standard.server.SPacketHandshake;
import net.daporkchop.fp2.core.server.FP2Server;
import net.daporkchop.fp2.core.server.player.IFarPlayerServer;
import net.daporkchop.fp2.core.util.threading.futureexecutor.FutureExecutor;

//? if neoforge {
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.TickEndEvent;
import net.daporkchop.fp2.mc.compat.vanilla.FP2Vanilla1_21;
import net.daporkchop.fp2.mc.server.player.FarPlayerServer1_21;
import net.daporkchop.fp2.mc.server.world.FColumn1_21;
import net.daporkchop.fp2.mc.server.world.FWorldServer1_21;
import net.daporkchop.fp2.mc.server.world.level.FLevelServer1_21;
import net.daporkchop.lib.math.vector.Vec2i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?}

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FP2Server1_21 extends FP2Server {
    private final FP2Core fp2;
    private final Map<UUID, IFarPlayerServer> players = new ConcurrentHashMap<>();

    //? if neoforge {
    private volatile FWorldServer1_21 world;
    //?}

    public FP2Server1_21(@NonNull FP2Core fp2) {
        this.fp2 = fp2;
    }

    @Override
    public void init(@NonNull FutureExecutor serverThreadExecutor) {
        super.init(serverThreadExecutor);
        //? if neoforge {
        NeoForge.EVENT_BUS.register(this);
        this.fp2.eventBus().register(new FP2Vanilla1_21());
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
    public void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        this.world = new FWorldServer1_21(this.fp2, server);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        FWorldServer1_21 w = this.world;
        this.world = null;
        if (w != null) {
            try {
                w.close();
            } catch (Exception e) {
                this.fp2.log().error("Error closing FWorldServer1_21", e);
            }
        }
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        FWorldServer1_21 w = this.world;
        if (w == null) return;
        Identifier id = Identifier.from(serverLevel.dimension().location().getNamespace(),
                serverLevel.dimension().location().getPath());
        w.loadLevel(id, serverLevel);
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        FWorldServer1_21 w = this.world;
        if (w == null) return;
        Identifier id = Identifier.from(serverLevel.dimension().location().getNamespace(),
                serverLevel.dimension().location().getPath());
        w.unloadLevel(id);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        IFarPlayerServer fp2Player = this.players.get(serverPlayer.getUUID());
        if (fp2Player == null) return;
        FWorldServer1_21 w = this.world;
        if (w == null) return;
        FLevelServer1_21 level = w.getLevel(serverLevel.dimension());
        if (level != null) {
            fp2Player.fp2_IFarPlayer_joinedWorld(level);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        FWorldServer1_21 w = this.world;
        if (w != null) {
            w.serverExecutor().tick();
            w.levelsByKey().values().forEach(level -> level.eventBus().fire(new TickEndEvent()));
            this.players.values().forEach(player -> player.fp2_IFarPlayer_update());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        FWorldServer1_21 w = this.world;
        if (w == null) return;
        FLevelServer1_21 fp2Level = w.getLevel(serverLevel.dimension());
        if (fp2Level == null) return;
        ChunkAccess chunk = event.getChunk();
        CompoundTag nbt = event.getData();
        fp2Level.eventBus().fire(new ColumnSavedEvent(
                Vec2i.of(chunk.getPos().x, chunk.getPos().z),
                new FColumn1_21(chunk.getPos(), nbt),
                nbt));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        FarPlayerServer1_21 fp2Player = new FarPlayerServer1_21(this.fp2, serverPlayer);
        this.players.put(serverPlayer.getUUID(), fp2Player);
        fp2Player.fp2_IFarPlayer_serverConfig(this.fp2.globalConfig());
        fp2Player.fp2_IFarPlayer_sendPacket(SPacketHandshake.create());

        // EntityJoinLevelEvent fires BEFORE PlayerLoggedInEvent in PlayerList.placeNewPlayer, so
        // onEntityJoinLevel sees no registered player on initial login. Wire the player's world
        // here too. onEntityJoinLevel still handles dimension changes after login.
        FWorldServer1_21 w = this.world;
        if (w != null) {
            FLevelServer1_21 level = w.getLevel(serverPlayer.serverLevel().dimension());
            if (level != null) {
                fp2Player.fp2_IFarPlayer_joinedWorld(level);
            }
        }
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
