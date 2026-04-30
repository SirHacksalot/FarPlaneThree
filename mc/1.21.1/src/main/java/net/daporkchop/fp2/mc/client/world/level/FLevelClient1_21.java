package net.daporkchop.fp2.mc.client.world.level;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Identifier;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.LevelRenderer;
import net.daporkchop.fp2.core.client.world.level.AbstractLevelClient;
import net.daporkchop.fp2.core.util.threading.workergroup.DefaultWorkerManager;
import net.daporkchop.fp2.core.util.threading.workergroup.WorkerManager;
import net.daporkchop.fp2.mc.client.render.LevelRenderer1_21;
import net.daporkchop.fp2.mc.client.world.FWorldClient1_21;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Getter
public class FLevelClient1_21 extends AbstractLevelClient<FP2Core, net.minecraft.client.Minecraft, FWorldClient1_21, ClientLevel, FLevelClient1_21> {
    private final IntAxisAlignedBB coordLimits;
    private volatile LevelRenderer1_21 renderer;

    public FLevelClient1_21(@NonNull FP2Core fp2, @NonNull ClientLevel implLevel, @NonNull FWorldClient1_21 world, @NonNull Identifier id, @NonNull IntAxisAlignedBB coordLimits) {
        super(fp2, implLevel, world, id);
        this.coordLimits = coordLimits;
    }

    @Override
    protected WorkerManager createWorkerManager() {
        return new DefaultWorkerManager(
                this.world().clientExecutor().thread(),
                this.world().clientExecutor());
    }

    @Override
    public FGameRegistry registry() {
        return this.world().gameRegistry();
    }

    @Override
    public long timestamp() {
        return this.implLevel().getGameTime();
    }

    @Override
    public LevelRenderer renderer() {
        LevelRenderer1_21 r = this.renderer;
        if (r == null) {
            synchronized (this) {
                r = this.renderer;
                if (r == null) {
                    r = new LevelRenderer1_21(Minecraft.getInstance(), this);
                    this.renderer = r;
                }
            }
        }
        return r;
    }
}
//?}
