package net.daporkchop.fp2.mc.asm.client.renderer;

//? if neoforge {
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes LevelRenderer's private viewArea and visibleSections fields for the FP2
// occlusion bridge (TerrainRenderingBlockedTracker1_21). visibleSections is the post-culling
// list of RenderSections rendered this frame — equivalent to 1.16's renderInfos.
@Mixin(LevelRenderer.class)
public interface AccessorLevelRenderer1_21 {
    @Accessor("viewArea")
    ViewArea fp2_getViewArea();

    @Accessor("visibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> fp2_getVisibleSections();

    @Accessor("lastCameraSectionX")
    int fp2_getLastCameraSectionX();

    @Accessor("lastCameraSectionY")
    int fp2_getLastCameraSectionY();

    @Accessor("lastCameraSectionZ")
    int fp2_getLastCameraSectionZ();

    @Accessor("sectionOcclusionGraph")
    SectionOcclusionGraph fp2_getSectionOcclusionGraph();
}
//?}
