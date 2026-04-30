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
import net.daporkchop.fp2.mc.client.render.Frustum1_21;
import net.daporkchop.fp2.mc.client.render.LightmapAccess1_21;
import net.daporkchop.fp2.mc.client.world.FWorldClient1_21;
import net.daporkchop.fp2.mc.compat.vanilla.FP2VanillaClient1_21;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
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
        // Register client-only vanilla integration (texUVs handlers etc.) on FP2's event bus.
        // Server-side instances must NOT load this class (Minecraft.getInstance() would NPE).
        this.fp2.eventBus().register(new FP2VanillaClient1_21());
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

        // prepare() flushes baked tile data into the renderIndex and selects which tiles to draw.
        // Must be called before render() each frame, otherwise drawableMask is permanently empty.
        renderer.prepare(this.fp2_cameraState, new Frustum1_21(event.getFrustum()));

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

        // Bind Minecraft's block atlas to TU0 (terrainTextureUnit) and lightmap to TU1
        // (lightmapTextureUnit) before render(). Without these, block.frag's `sampleTerrain()`
        // and `texture(LIGHTMAP_SAMPLER, ...)` read garbage / zeros and the fragment color
        // collapses to black. Vanilla rebinds these to its own texture units between render
        // stages, so we have to set them on FP2's expected units here. Save/restore the prior
        // GL bindings so we don't perturb subsequent vanilla render stages.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        int atlasId = mc.getModelManager().getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS).getId();
        int lightmapId = LightmapAccess1_21.textureId(mc.gameRenderer.lightTexture());
        int prevActiveTU = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int prevTU0Binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, atlasId);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        int prevTU1Binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightmapId);
        try {
            renderer.render(this.fp2_cameraState, drawState);
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTU1Binding);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTU0Binding);
            GL13.glActiveTexture(prevActiveTU);
        }
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
