package net.daporkchop.fp2.mc.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.common.util.ResourceProvider;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.gui.GuiContext;
import net.daporkchop.fp2.core.client.gui.GuiScreen;
import net.daporkchop.fp2.core.client.key.KeyCategory;
import net.daporkchop.fp2.core.client.listener.FramebufferResizeListener;
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.client.render.RenderManager;
import net.daporkchop.fp2.core.log4j.util.log.Log4jAsPorkLibLogger;
import org.apache.logging.log4j.LogManager;

//? if neoforge {
import net.daporkchop.fp2.mc.client.player.FarPlayerClient1_21;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
//?}

import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.api.FP2.*;

@Getter
public class FP2Client1_21 extends FP2Client {
    private final FP2Core fp2;

    //? if neoforge {
    private volatile FarPlayerClient1_21 currentPlayerInstance;
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
        FarPlayerClient1_21 player = new FarPlayerClient1_21(this.fp2);
        this.currentPlayerInstance = player;
    }

    @SubscribeEvent
    public void onPlayerLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FarPlayerClient1_21 player = this.currentPlayerInstance;
        this.currentPlayerInstance = null;
        if (player != null) {
            player.close();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceProvider resourceProvider() {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public int vanillaRenderDistanceChunks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int terrainTextureUnit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lightmapTextureUnit() {
        throw new UnsupportedOperationException();
    }
}
