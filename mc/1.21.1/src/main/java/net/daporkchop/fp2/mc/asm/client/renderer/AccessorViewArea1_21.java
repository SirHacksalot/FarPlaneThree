package net.daporkchop.fp2.mc.asm.client.renderer;

//? if neoforge {
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes ViewArea's protected sectionGridSizeX/Y/Z so the occlusion bridge can compute the
// section grid extents. The `sections` array field is already public.
@Mixin(ViewArea.class)
public interface AccessorViewArea1_21 {
    @Accessor("sectionGridSizeX")
    int fp2_getSectionGridSizeX();

    @Accessor("sectionGridSizeY")
    int fp2_getSectionGridSizeY();

    @Accessor("sectionGridSizeZ")
    int fp2_getSectionGridSizeZ();
}
//?}
