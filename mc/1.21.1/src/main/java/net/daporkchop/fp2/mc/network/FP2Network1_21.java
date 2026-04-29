package net.daporkchop.fp2.mc.network;

//? if neoforge {
import lombok.NonNull;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.core.network.RegisterPacketsEvent;
import net.daporkchop.fp2.mc.client.FP2Client1_21;
import net.daporkchop.fp2.mc.server.FP2Server1_21;
import net.daporkchop.lib.binary.stream.DataIn;
import net.daporkchop.lib.binary.stream.DataOut;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.daporkchop.lib.common.util.PValidation.*;

public final class FP2Network1_21 {
    private static final Logger LOGGER = LogManager.getLogger(FP2Network1_21.class);

    private static volatile boolean INITIALIZED = false;
    private static MethodHandle[] clientboundCtors;
    private static MethodHandle[] serverboundCtors;
    private static Map<Class<?>, Integer> clientboundIds;
    private static Map<Class<?>, Integer> serverboundIds;

    private FP2Network1_21() {}

    public static synchronized void init(@NonNull FP2Core fp2, @NonNull RegisterPayloadHandlersEvent event) {
        checkState(!INITIALIZED, "FP2Network1_21 already initialized!");
        INITIALIZED = true;

        List<Class<? extends IPacket>> clientboundClasses = new ArrayList<>();
        List<Class<? extends IPacket>> serverboundClasses = new ArrayList<>();

        fp2.eventBus().fire(new RegisterPacketsEvent() {
            @Override
            public RegisterPacketsEvent registerClientbound(@NonNull Class<? extends IPacket> clazz) {
                clientboundClasses.add(clazz);
                return this;
            }

            @Override
            public RegisterPacketsEvent registerServerbound(@NonNull Class<? extends IPacket> clazz) {
                serverboundClasses.add(clazz);
                return this;
            }
        });

        clientboundCtors = buildCtors(clientboundClasses);
        serverboundCtors = buildCtors(serverboundClasses);
        clientboundIds = buildIdMap(clientboundClasses);
        serverboundIds = buildIdMap(serverboundClasses);

        LOGGER.info("FP2: registered {} clientbound and {} serverbound packet types",
                clientboundClasses.size(), serverboundClasses.size());

        MethodHandle[] cbCtors = clientboundCtors;
        MethodHandle[] sbCtors = serverboundCtors;
        Map<Class<?>, Integer> cbIds = clientboundIds;
        Map<Class<?>, Integer> sbIds = serverboundIds;

        StreamCodec<RegistryFriendlyByteBuf, FP2ClientboundPayload> clientboundCodec = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeShort(cbIds.get(payload.packet().getClass()));
                    try {
                        payload.packet().write(DataOut.wrap(buf, false));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encode clientbound packet", e);
                    }
                },
                buf -> {
                    int id = buf.readUnsignedShort();
                    IPacket packet = invokeDefaultCtor(cbCtors[id]);
                    try {
                        packet.read(DataIn.wrap(buf, false));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to decode clientbound packet id=" + id, e);
                    }
                    return new FP2ClientboundPayload(packet);
                }
        );

        StreamCodec<RegistryFriendlyByteBuf, FP2ServerboundPayload> serverboundCodec = StreamCodec.of(
                (buf, payload) -> {
                    buf.writeShort(sbIds.get(payload.packet().getClass()));
                    try {
                        payload.packet().write(DataOut.wrap(buf, false));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encode serverbound packet", e);
                    }
                },
                buf -> {
                    int id = buf.readUnsignedShort();
                    IPacket packet = invokeDefaultCtor(sbCtors[id]);
                    try {
                        packet.read(DataIn.wrap(buf, false));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to decode serverbound packet id=" + id, e);
                    }
                    return new FP2ServerboundPayload(packet);
                }
        );

        PayloadRegistrar registrar = event.registrar(FP2Core.MODID);

        registrar.playToClient(FP2ClientboundPayload.TYPE, clientboundCodec,
                (payload, context) -> {
                    IPacket packet = payload.packet();
                    LOGGER.debug("FP2: client received packet: {}", packet.getClass().getSimpleName());
                    FP2Client1_21 client = (FP2Client1_21) fp2.client();
                    client.currentPlayer().ifPresent(player -> player.handle(packet));
                });

        registrar.playToServer(FP2ServerboundPayload.TYPE, serverboundCodec,
                (payload, context) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) context.player();
                    FP2Server1_21 server = (FP2Server1_21) fp2.server();
                    server.handleIncoming(serverPlayer.getUUID(), payload.packet());
                });
    }

    private static MethodHandle[] buildCtors(@NonNull List<Class<? extends IPacket>> classes) {
        MethodHandle[] ctors = new MethodHandle[classes.size()];
        try {
            for (int i = 0; i < classes.size(); i++) {
                ctors[i] = MethodHandles.publicLookup()
                        .findConstructor(classes.get(i), MethodType.methodType(void.class));
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find default constructor for packet class", e);
        }
        return ctors;
    }

    private static Map<Class<?>, Integer> buildIdMap(@NonNull List<Class<? extends IPacket>> classes) {
        Map<Class<?>, Integer> map = new IdentityHashMap<>();
        for (int i = 0; i < classes.size(); i++) {
            map.put(classes.get(i), i);
        }
        return map;
    }

    private static IPacket invokeDefaultCtor(MethodHandle ctor) {
        try {
            return (IPacket) ctor.invoke();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to construct packet instance", t);
        }
    }

    public static void sendToPlayer(@NonNull IPacket packet, @NonNull ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FP2ClientboundPayload(packet));
    }

    public static void sendToPlayer(@NonNull IPacket packet, @NonNull ServerPlayer player, Consumer<Throwable> handler) {
        sendToPlayer(packet, player);
    }

    public static void sendToServer(@NonNull IPacket packet) {
        PacketDistributor.sendToServer(new FP2ServerboundPayload(packet));
    }
}
//?}
