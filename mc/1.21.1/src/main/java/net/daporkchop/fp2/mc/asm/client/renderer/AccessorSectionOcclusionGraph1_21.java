package net.daporkchop.fp2.mc.asm.client.renderer;

//? if neoforge {
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.atomic.AtomicReference;

// Exposes SectionOcclusionGraph.currentGraph so the occlusion bridge can walk per-section
// graph-traversal state. The inner GraphState is a record (storage() is public); GraphStorage
// has public fields (renderSections is a LinkedHashSet<Node>).
@Mixin(SectionOcclusionGraph.class)
public interface AccessorSectionOcclusionGraph1_21 {
    @Accessor("currentGraph")
    AtomicReference<Object> fp2_getCurrentGraph();
}
//?}
