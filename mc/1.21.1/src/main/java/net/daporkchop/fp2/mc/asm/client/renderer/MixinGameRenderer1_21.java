package net.daporkchop.fp2.mc.asm.client.renderer;

//? if neoforge {
import net.daporkchop.fp2.core.client.player.IFarPlayerClient;
import net.daporkchop.fp2.core.config.FP2Config;
import net.daporkchop.fp2.core.engine.api.ctx.IFarClientContext;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.daporkchop.fp2.core.FP2Core.fp2;

// Replaces the far-plane distance used in GameRenderer.getProjectionMatrix() with the larger of
// vanilla's value and FP2's effective render distance, so the projection matrix doesn't clip
// FP2 LOD chunks past vanilla's view distance. Only the projection matrix call site is affected
// — other callers of getDepthFar() (fog, frustum culling, etc.) keep vanilla behavior.
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer1_21 {
    @Redirect(method = "getProjectionMatrix",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getDepthFar()F"),
            require = 1, allow = 1)
    private float fp2_getProjectionMatrix_extendFarPlane(GameRenderer self) {
        float vanillaFar = self.getDepthFar();
        IFarPlayerClient player = fp2().client().currentPlayer().orElse(null);
        if (player == null) {
            return vanillaFar;
        }
        IFarClientContext ctx = player.activeContext();
        if (ctx == null) {
            return vanillaFar;
        }
        FP2Config cfg = ctx.config();
        if (cfg == null) {
            return vanillaFar;
        }
        return Math.max(vanillaFar, (float) cfg.effectiveRenderDistanceBlocks());
    }
}
//?}
