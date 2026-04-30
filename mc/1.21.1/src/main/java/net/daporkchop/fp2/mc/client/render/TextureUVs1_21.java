package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.FP2Core;
import net.daporkchop.fp2.core.client.render.common.AbstractTextureUVs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Collections;
import java.util.List;

@Getter
public class TextureUVs1_21 extends AbstractTextureUVs {
    protected final Minecraft mc;

    public TextureUVs1_21(@NonNull FP2Core fp2, @NonNull FGameRegistry registry, @NonNull Minecraft mc) {
        super(fp2, registry);
        this.mc = mc;

        this.reloadUVs();
    }

    @Override
    protected List<PackedBakedQuad> missingTextureQuads() {
        TextureAtlasSprite sprite = this.mc.getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(MissingTextureAtlasSprite.getLocation());
        return Collections.singletonList(new PackedBakedQuad(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), 1.0f));
    }
}
//?}
