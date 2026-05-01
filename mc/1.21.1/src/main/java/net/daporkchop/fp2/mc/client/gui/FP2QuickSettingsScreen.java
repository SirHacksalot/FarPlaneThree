package net.daporkchop.fp2.mc.client.gui;

//? if neoforge {
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.EngineConstants;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Lightweight in-game tweak screen for the few FP2 settings users actually adjust live:
 * cutoff distance, max LOD levels, GPU frustum culling, and the terrain worker count. Click
 * Done to apply (rebuilds {@link FP2Config} via Lombok {@code @With} setters and calls
 * {@link FP2Core#globalConfig(FP2Config)}); Cancel discards.
 *
 * <p>Not a port of FP2's full reflective config GUI — that screen drives every setting from
 * annotations and needs an MC-side {@code GuiContext}/{@code GuiRenderer} bridge. This screen
 * exists so cutoff/levels can be tuned without restarting; the rest stays in {@code fp2.json5}.
 */
public final class FP2QuickSettingsScreen extends Screen {
    private static final int WIDGET_WIDTH = 200;
    private static final int WIDGET_HEIGHT = 20;
    private static final int WIDGET_GAP = 4;

    private static final int CUTOFF_MIN = EngineConstants.T_VOXELS;       // 16
    private static final int CUTOFF_MAX = 1024;
    private static final int CUTOFF_STEP = EngineConstants.T_VOXELS;

    private static final int LEVELS_MIN = 1;
    private static final int LEVELS_MAX = EngineConstants.MAX_LODS;

    private static final int THREADS_MIN = 1;
    private static final int THREADS_MAX = 32;

    private final Screen parent;
    private final FP2Config startingConfig;

    // Mutable working state — written by widgets, applied on Done.
    private int pendingCutoffDistance;
    private int pendingMaxLevels;
    private int pendingTerrainThreads;
    private boolean pendingGpuFrustumCulling;

    public FP2QuickSettingsScreen(Screen parent) {
        super(Component.literal("FP2 Quick Settings"));
        this.parent = parent;
        this.startingConfig = FP2Core.fp2().globalConfig();
        this.pendingCutoffDistance = this.startingConfig.cutoffDistance();
        this.pendingMaxLevels = this.startingConfig.maxLevels();
        this.pendingTerrainThreads = this.startingConfig.performance().terrainThreads();
        this.pendingGpuFrustumCulling = this.startingConfig.performance().gpuFrustumCulling();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int firstY = this.height / 4;
        int row = 0;

        this.addRenderableWidget(new IntSlider(
                centerX - WIDGET_WIDTH / 2, firstY + row++ * (WIDGET_HEIGHT + WIDGET_GAP),
                "Cutoff distance", CUTOFF_MIN, CUTOFF_MAX, CUTOFF_STEP,
                this.pendingCutoffDistance, value -> this.pendingCutoffDistance = value));

        this.addRenderableWidget(new IntSlider(
                centerX - WIDGET_WIDTH / 2, firstY + row++ * (WIDGET_HEIGHT + WIDGET_GAP),
                "Max LOD levels", LEVELS_MIN, LEVELS_MAX, 1,
                this.pendingMaxLevels, value -> this.pendingMaxLevels = value));

        this.addRenderableWidget(new IntSlider(
                centerX - WIDGET_WIDTH / 2, firstY + row++ * (WIDGET_HEIGHT + WIDGET_GAP),
                "Terrain threads", THREADS_MIN, THREADS_MAX, 1,
                this.pendingTerrainThreads, value -> this.pendingTerrainThreads = value));

        this.addRenderableWidget(CycleButton.onOffBuilder(this.pendingGpuFrustumCulling)
                .create(centerX - WIDGET_WIDTH / 2, firstY + row++ * (WIDGET_HEIGHT + WIDGET_GAP),
                        WIDGET_WIDTH, WIDGET_HEIGHT,
                        Component.literal("GPU frustum culling"),
                        (button, value) -> this.pendingGpuFrustumCulling = value));

        int doneCancelY = firstY + (row + 1) * (WIDGET_HEIGHT + WIDGET_GAP);
        int halfWidth = (WIDGET_WIDTH - WIDGET_GAP) / 2;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.applyAndClose())
                .bounds(centerX - WIDGET_WIDTH / 2, doneCancelY, halfWidth, WIDGET_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(centerX + WIDGET_GAP / 2, doneCancelY, halfWidth, WIDGET_HEIGHT)
                .build());
    }

    private void applyAndClose() {
        FP2Config updated = this.startingConfig
                .withMaxLevels(this.pendingMaxLevels)
                .withCutoffDistance(this.pendingCutoffDistance)
                .withPerformance(this.startingConfig.performance()
                        .withGpuFrustumCulling(this.pendingGpuFrustumCulling)
                        .withTerrainThreads(this.pendingTerrainThreads));
        FP2Core.fp2().globalConfig(updated);
        this.onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    /**
     * Integer-valued slider over a closed range, snapping to {@code step}. Reports each commit via
     * the supplied callback so the screen's pending state stays in sync.
     */
    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min;
        private final int max;
        private final int step;
        private final java.util.function.IntConsumer onValueChanged;

        IntSlider(int x, int y, String label, int min, int max, int step,
                  int initialValue, java.util.function.IntConsumer onValueChanged) {
            super(x, y, WIDGET_WIDTH, WIDGET_HEIGHT, Component.empty(),
                    fractionFromValue(min, max, initialValue));
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.onValueChanged = onValueChanged;
            this.updateMessage();
        }

        private static double fractionFromValue(int min, int max, int value) {
            if (max == min) {
                return 0.0;
            }
            return (double) (value - min) / (double) (max - min);
        }

        private int currentValue() {
            int raw = (int) Math.round(this.min + this.value * (this.max - this.min));
            int snapped = Math.round((float) raw / this.step) * this.step;
            return Math.max(this.min, Math.min(this.max, snapped));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(this.label + ": " + this.currentValue()));
        }

        @Override
        protected void applyValue() {
            this.onValueChanged.accept(this.currentValue());
        }
    }
}
//?}
