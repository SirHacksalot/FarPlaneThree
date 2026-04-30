package net.daporkchop.fp2.mc.compat.vanilla;

//? if neoforge {
import net.daporkchop.fp2.api.event.Constrain;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.core.client.render.TextureUVs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Client-only handlers — Minecraft.getInstance() and friends are unavailable server-side, so this
// class is registered from FP2Client1_21 only. Mirror of 1.12.2's FP2Vanilla1_12 texUVs handlers.
public class FP2VanillaClient1_21 {
    private static net.minecraft.core.Direction toMcDirection(Direction d) {
        switch (d) {
            case POSITIVE_X: return net.minecraft.core.Direction.EAST;
            case NEGATIVE_X: return net.minecraft.core.Direction.WEST;
            case POSITIVE_Y: return net.minecraft.core.Direction.UP;
            case NEGATIVE_Y: return net.minecraft.core.Direction.DOWN;
            case POSITIVE_Z: return net.minecraft.core.Direction.SOUTH;
            case NEGATIVE_Z: return net.minecraft.core.Direction.NORTH;
            default: throw new IllegalArgumentException(d.name());
        }
    }

    private static Optional<List<TextureUVs.PackedBakedQuad>> fluidQuad(String stillTexture, String flowTexture, Direction faceDir) {
        // Use the still sprite for Y faces and flow for side faces, mirroring 1.12.2's choice.
        // Sprite UVs are sized to a 16x16 (still) or 8x8 (flow) cell; passing the sprite's full
        // (u0,v0)-(u1,v1) rectangle is sufficient since FP2 doesn't animate.
        ResourceLocation loc = ResourceLocation.parse(faceDir.x() != 0 || faceDir.z() != 0 ? flowTexture : stillTexture);
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager()
                .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                .getSprite(loc);
        return Optional.of(Collections.singletonList(new TextureUVs.PackedBakedQuad(
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), 0.0f)));
    }

    @FEventHandler(name = "vanilla_texuvs_renderquads_water",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsWater(TextureUVs.StateFaceQuadRenderEvent event) {
        BlockState state = (BlockState) event.registry().id2state(event.state());
        Block block = state.getBlock();
        if (block != Blocks.WATER) {
            return Optional.empty();
        }
        return fluidQuad("minecraft:block/water_still", "minecraft:block/water_flow", event.direction());
    }

    @FEventHandler(name = "vanilla_texuvs_renderquads_lava",
            constrain = @Constrain(before = "vanilla_texuvs_renderquads_default"))
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsLava(TextureUVs.StateFaceQuadRenderEvent event) {
        BlockState state = (BlockState) event.registry().id2state(event.state());
        Block block = state.getBlock();
        if (block != Blocks.LAVA) {
            return Optional.empty();
        }
        return fluidQuad("minecraft:block/lava_still", "minecraft:block/lava_flow", event.direction());
    }

    @FEventHandler(name = "vanilla_texuvs_renderquads_default")
    public Optional<List<TextureUVs.PackedBakedQuad>> texUVsRenderQuadsDefault(TextureUVs.StateFaceQuadRenderEvent event) {
        BlockState state = (BlockState) event.registry().id2state(event.state());
        net.minecraft.core.Direction facing = toMcDirection(event.direction());

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        // Stable seed: 0L matches what FP2 1.12.2 uses; vanilla itself uses position-derived seeds
        // for runtime rendering (variant randomization), but for atlas-UV extraction a fixed seed
        // produces deterministic, repeatable quad selection across launches.
        RandomSource random = RandomSource.create(0L);

        List<BakedQuad> quads = model.getQuads(state, facing, random);
        if (quads.isEmpty()) {
            // Block model has no cullface for this side — search non-culled quads for a matching face.
            for (BakedQuad quad : model.getQuads(state, null, random)) {
                if (quad.getDirection() == facing) {
                    quads = Collections.singletonList(quad);
                    break;
                }
            }
        }

        if (quads.isEmpty()) {
            return Optional.empty();
        }

        List<TextureUVs.PackedBakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite sprite = quad.getSprite();
            float tintFactor = quad.getTintIndex() < 0 ? 1.0f : 0.0f;
            out.add(new TextureUVs.PackedBakedQuad(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), tintFactor));
        }
        return Optional.of(out);
    }
}
//?}
