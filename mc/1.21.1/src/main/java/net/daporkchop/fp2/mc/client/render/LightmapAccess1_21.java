package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.SneakyThrows;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.lang.reflect.Field;

// Provides the OpenGL texture ID of Minecraft's lightmap so FP2 can bind it to its expected
// texture unit. LightTexture.lightTexture is private; PUnsafe lets us read it without going
// through Field.setAccessible (which would require --add-opens of net.minecraft.client.renderer).
public final class LightmapAccess1_21 {
    private static final long LIGHT_TEXTURE_FIELD_OFFSET = fieldOffset();

    @SneakyThrows(NoSuchFieldException.class)
    private static long fieldOffset() {
        Field f = LightTexture.class.getDeclaredField("lightTexture");
        return PUnsafe.objectFieldOffset(f);
    }

    public static int textureId(LightTexture lightTexture) {
        DynamicTexture dyn = (DynamicTexture) PUnsafe.getObject(lightTexture, LIGHT_TEXTURE_FIELD_OFFSET);
        return dyn.getId();
    }

    private LightmapAccess1_21() {}
}
//?}
