package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.SneakyThrows;
import net.daporkchop.fp2.mc.asm.client.renderer.AccessorSectionOcclusionGraph1_21;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.renderer.SectionOcclusionGraph;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

// Navigates from SectionOcclusionGraph to its private GraphStorage.renderSections set.
// GraphState and GraphStorage are package-private inner classes — we can't reference them by
// Java type from outside, so we go through reflection. Method handle + field offset are looked
// up once at class init; PUnsafe.getObject bypasses setAccessible (same trick as
// DirectBufferHackery — works without --add-opens).
public final class SectionGraphAccess1_21 {
    private static final Method GRAPH_STATE_STORAGE_METHOD;
    private static final long GRAPH_STORAGE_RENDER_SECTIONS_OFFSET;

    static {
        try {
            Class<?> graphStateClass = Class.forName("net.minecraft.client.renderer.SectionOcclusionGraph$GraphState");
            Class<?> graphStorageClass = Class.forName("net.minecraft.client.renderer.SectionOcclusionGraph$GraphStorage");
            GRAPH_STATE_STORAGE_METHOD = graphStateClass.getDeclaredMethod("storage"); //record accessor, public
            Field renderSectionsField = graphStorageClass.getDeclaredField("renderSections"); //public field
            GRAPH_STORAGE_RENDER_SECTIONS_OFFSET = PUnsafe.objectFieldOffset(renderSectionsField);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * @return the LinkedHashSet of SectionOcclusionGraph$Node currently in the graph, typed as
     *         Collection&lt;Object&gt; because Node has package-private source access. Caller
     *         must cast each element to the AccessorSectionOcclusionGraphNode1_21 mixin
     *         interface to read its fields. Returns null if the graph hasn't initialised yet.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static Collection<Object> renderSections(SectionOcclusionGraph graph) {
        Object state = ((AccessorSectionOcclusionGraph1_21) graph).fp2_getCurrentGraph().get();
        if (state == null) return null;
        Object storage = GRAPH_STATE_STORAGE_METHOD.invoke(state);
        if (storage == null) return null;
        return (Collection<Object>) PUnsafe.getObject(storage, GRAPH_STORAGE_RENDER_SECTIONS_OFFSET);
    }

    private SectionGraphAccess1_21() {}
}
//?}
