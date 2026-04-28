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

import java.util.Optional;
import java.util.function.Function;

import static net.daporkchop.fp2.api.FP2.*;

@Getter
public class FP2Client1_21 extends FP2Client {
    private final FP2Core fp2;

    public FP2Client1_21(@NonNull FP2Core fp2) {
        this.fp2 = fp2;
        this.chat(new Log4jAsPorkLibLogger(LogManager.getLogger(MODID + ".chat")));
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
    public Optional<? extends IFarPlayerClient> currentPlayer() {
        return Optional.empty();
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
