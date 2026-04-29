package net.daporkchop.fp2.mc.network;

//? if neoforge {
import net.daporkchop.fp2.core.network.IPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static net.daporkchop.fp2.api.FP2.*;

public record FP2ClientboundPayload(IPacket packet) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<FP2ClientboundPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "s2c"));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
//?}
