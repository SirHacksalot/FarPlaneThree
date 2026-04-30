package net.daporkchop.fp2.mc.asm.client.renderer;

//? if neoforge {
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes SectionOcclusionGraph$Node's private direction bitmasks. `directions` is the bitmask
// of neighbors this node will traverse to (1.16-equivalent: hasDirection); `sourceDirections`
// is the bitmask of neighbors this node was entered from (1.16-equivalent: getSourceDirection,
// but a bitmask in 1.21 since multiple paths can reach the same node).
//
// Target via fully-qualified string because Node has package-private access at the Java source
// level (despite javap reporting "public class") — referencing it directly via .class won't
// compile from outside the package.
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph$Node")
public interface AccessorSectionOcclusionGraphNode1_21 {
    @Accessor("directions")
    byte fp2_getDirections();

    @Accessor("sourceDirections")
    byte fp2_getSourceDirections();

    @Accessor("section")
    SectionRenderDispatcher.RenderSection fp2_getSection();
}
//?}
