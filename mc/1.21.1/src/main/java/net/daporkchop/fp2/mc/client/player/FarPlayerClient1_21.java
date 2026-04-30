package net.daporkchop.fp2.mc.client.player;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.player.AbstractFarPlayerClient;
import net.daporkchop.fp2.core.client.world.level.IFarLevelClient;
import net.daporkchop.fp2.core.network.IPacket;
import net.daporkchop.fp2.mc.client.world.FWorldClient1_21;
import net.daporkchop.fp2.mc.client.world.level.FLevelClient1_21;
import net.daporkchop.fp2.mc.network.FP2Network1_21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Getter
public class FarPlayerClient1_21 extends AbstractFarPlayerClient<FP2Core> {
    private final FP2Core fp2;
    private final FWorldClient1_21 world;

    public FarPlayerClient1_21(@NonNull FP2Core fp2, @NonNull FWorldClient1_21 world) {
        this.fp2 = fp2;
        this.world = world;
        this.ready();
    }

    @Override
    protected FLevelClient1_21 loadActiveLevel() {
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            throw new IllegalStateException("no active client level when session began");
        }
        Identifier id = Identifier.from(
                clientLevel.dimension().location().getNamespace(),
                clientLevel.dimension().location().getPath());
        return this.world.loadLevel(id, clientLevel);
    }

    @Override
    public void send(@NonNull IPacket packet) {
        FP2Network1_21.sendToServer(packet);
    }
}
//?}
