package net.daporkchop.fp2.mc.client.world;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.world.AbstractWorldClient;
import net.daporkchop.fp2.mc.client.world.level.FLevelClient1_21;
import net.daporkchop.fp2.mc.util.threading.futureexecutor.ClientThreadMarkedFutureExecutor1_21;
import net.daporkchop.fp2.mc.world.registry.GameRegistry1_21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Getter
public class FWorldClient1_21 extends AbstractWorldClient<FP2Core, Minecraft, FWorldClient1_21, ClientLevel, FLevelClient1_21> {
    private final ClientThreadMarkedFutureExecutor1_21 clientExecutor;
    private final FGameRegistry gameRegistry;

    public FWorldClient1_21(@NonNull FP2Core fp2) {
        super(fp2, Minecraft.getInstance());
        this.clientExecutor = new ClientThreadMarkedFutureExecutor1_21();
        this.gameRegistry = new GameRegistry1_21(Minecraft.getInstance().getConnection().registryAccess());
    }

    @Override
    protected FLevelClient1_21 createLevel(@NonNull Identifier id, @NonNull ClientLevel implLevel) {
        return new FLevelClient1_21(this.fp2(), implLevel, this, id, COORD_LIMITS_HACK.get());
    }

    @Override
    protected void doClose() throws Exception {
        try (AutoCloseable closeSuper = super::doClose) {
            this.clientExecutor.close();
        }
    }
}
//?}
