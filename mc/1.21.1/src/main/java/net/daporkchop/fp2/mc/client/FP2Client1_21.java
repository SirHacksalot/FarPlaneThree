package net.daporkchop.fp2.mc.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.listener.FramebufferResizeListener;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.client.render.RenderManager;
import net.daporkchop.fp2.core.client.render.ReversedZ;
import net.daporkchop.fp2.core.log4j.util.log.Log4jAsPorkLibLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;

//? if neoforge {
import net.daporkchop.fp2.core.client.render.state.CameraState;
import net.daporkchop.fp2.core.client.render.state.DrawState;
import net.daporkchop.fp2.core.engine.api.ctx.IFarClientContext;
import net.daporkchop.fp2.core.engine.client.AbstractFarRenderer;
import net.daporkchop.fp2.mc.client.player.FarPlayerClient1_21;
import net.daporkchop.fp2.mc.client.world.FWorldClient1_21;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
//?}

import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.api.FP2.*;

@Getter
public class FP2Client1_21 extends FP2Client {
    private final FP2Core fp2;

    //? if neoforge {
    private volatile FWorldClient1_21 currentWorld;
    private volatile FarPlayerClient1_21 currentPlayerInstance;

    private final CameraState fp2_cameraState = new CameraState();
    private final float[] fp2_modelView = new float[16];
    private final float[] fp2_projection = new float[16];
    //?}

    public FP2Client1_21(@NonNull FP2Core fp2) {
        this.fp2 = fp2;
        this.chat(new Log4jAsPorkLibLogger(LogManager.getLogger(MODID + ".chat")));
        //? if neoforge {
        NeoForge.EVENT_BUS.register(this);
        //?}
    }

    //? if neoforge {
    @SubscribeEvent
    public void onPlayerLogIn(ClientPlayerNetworkEvent.LoggingIn event) {
        FWorldClient1_21 world = new FWorldClient1_21(this.fp2);
        FarPlayerClient1_21 player = new FarPlayerClient1_21(this.fp2, world);
        this.currentWorld = world;
        this.currentPlayerInstance = player;
    }

    @SubscribeEvent
    public void onPlayerLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FarPlayerClient1_21 player = this.currentPlayerInstance;
        FWorldClient1_21 world = this.currentWorld;
        this.currentPlayerInstance = null;
        this.currentWorld = null;
        if (player != null) {
            player.close();
        }
        if (world != null) {
            try {
                world.close();
            } catch (Exception e) {
                this.fp2.log().error("Error closing FWorldClient1_21", e);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) return;
        FWorldClient1_21 world = this.currentWorld;
        if (world != null) {
            world.clientExecutor().tick();
        }
    }

    // Per-frame render hook — drives AbstractFarRenderer.render() at the same point in the
    // pipeline 1.12.2's MixinEntityRenderer1_12 used (after vanilla cutout). MVP: minimal
    // DrawState (fog disabled, alpha cutout 0.1f), no far-plane override yet — distant FP2
    // chunks will still be clipped by vanilla's projection matrix. Wire those next.
    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;

        FarPlayerClient1_21 player = this.currentPlayerInstance;
        if (player == null) return;
        IFarClientContext context = player.activeContext();
        if (context == null) return;
        AbstractFarRenderer renderer = context.renderer();
        if (renderer == null) return;

        event.getModelViewMatrix().get(this.fp2_modelView);
        event.getProjectionMatrix().get(this.fp2_projection);
        this.fp2_cameraState.zNear = 0.05f;
        this.fp2_cameraState.setModelViewMatrixAndProjectionMatrix(this.fp2_modelView, this.fp2_projection, this);
        Vec3 pos = event.getCamera().getPosition();
        this.fp2_cameraState.positionDouble(pos.x, pos.y, pos.z);

        DrawState drawState = new DrawState();
        drawState.fogMode = DrawState.FogMode.DISABLED;
        drawState.fogStart = 0.0f;
        drawState.fogEnd = 1.0f;
        drawState.fogDensity = 0.0f;
        drawState.fogColorR = 0.0f;
        drawState.fogColorG = 0.0f;
        drawState.fogColorB = 0.0f;
        drawState.fogColorA = 1.0f;
        drawState.alphaRefCutout = 0.1f;

        renderer.render(this.fp2_cameraState, drawState);
    }
    //?}

    @Override
    public Optional<? extends IFarPlayerClient> currentPlayer() {
        //? if neoforge {
        return Optional.ofNullable(this.currentPlayerInstance);
        //?} else {
        /*return Optional.empty();*/
        //?}
    }

    @Override
    protected RenderManager createRenderManager() {
        return new RenderManager(this) {
            @Override
            protected ReversedZ createReversedZ(@NonNull FP2Client client) {
                return null;
            }
        };
    }

    @Override
    public ResourceProvider resourceProvider() {
        Minecraft mc = Minecraft.getInstance();
        return id -> {
            var resource = mc.getResourceManager().getResource(ResourceLocation.parse(id.toString()));
            if (resource.isEmpty()) {
                throw new ResourceNotFoundException(id);
            }
            return resource.get().open();
        };
    }

    @Override
    public <T extends GuiScreen> T openScreen(@NonNull Function<GuiContext, T> factory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public KeyCategory createKeyCategory(@NonNull String localeKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void fireFramebufferResize(@NonNull FramebufferResizeListener listener) {
        Minecraft mc = Minecraft.getInstance();
        listener.onFramebufferResize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }

    @Override
    public int vanillaRenderDistanceChunks() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    @Override
    public int terrainTextureUnit() {
        return 0;
    }

    @Override
    public int lightmapTextureUnit() {
        return 1;
    }
}
