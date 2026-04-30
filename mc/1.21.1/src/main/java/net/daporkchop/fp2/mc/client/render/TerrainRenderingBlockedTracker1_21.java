package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.NonNull;
import net.daporkchop.fp2.core.client.FP2Client;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.mc.asm.client.renderer.AccessorLevelRenderer1_21;
import net.daporkchop.fp2.mc.asm.client.renderer.AccessorSectionOcclusionGraphNode1_21;
import net.daporkchop.fp2.mc.asm.client.renderer.AccessorViewArea1_21;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Collection;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Bridges 1.21's section-graph state to FP2's TerrainRenderingBlockedTracker bit-flag layout.
// Per-section graph-traversal direction data is recovered from SectionOcclusionGraph$Node via
// SectionGraphAccess1_21 (reflection through package-private GraphState/GraphStorage records).
// Each Node exposes `directions` (bitmask of neighbors this node will traverse to —
// 1.16-equivalent: hasDirection) and `sourceDirections` (bitmask of neighbors this node was
// entered from — 1.16-equivalent: getSourceDirection). We pick the lowest set bit of
// sourceDirections as the canonical inFace; transformFlags uses it to skip the inFace direction
// when scanning neighbors. With these flags populated, the algorithm matches 1.16's behavior
// and the boundary overlap artifacts shrink to the actual edge of vanilla's render distance.
public class TerrainRenderingBlockedTracker1_21 extends TerrainRenderingBlockedTracker {
    protected static final Direction[] DIRECTIONS = Direction.values();

    protected static long visibilityFlags(SectionRenderDispatcher.CompiledSection compiled) {
        long flags = 0L;
        int shift = SHIFT_VISIBILITY;
        for (Direction outFace : DIRECTIONS) {
            for (Direction inFace : DIRECTIONS) {
                if (compiled.facesCanSeeEachother(inFace, outFace)) {
                    flags |= 1L << shift;
                }
                shift++;
            }
        }
        return flags;
    }

    public TerrainRenderingBlockedTracker1_21(FP2Client client) {
        super(client);
    }

    @Override
    protected int[] getExpectedFaceOffsets() {
        return Stream.of(DIRECTIONS)
                .flatMapToInt(d -> IntStream.of(d.getStepX(), d.getStepY(), d.getStepZ()))
                .toArray();
    }

    public void update(@NonNull LevelRenderer levelRenderer, @NonNull Frustum frustum) {
        ViewArea viewArea = ((AccessorLevelRenderer1_21) levelRenderer).fp2_getViewArea();
        if (viewArea == null || viewArea.sections == null) {
            return; //level not ready
        }

        AccessorViewArea1_21 viewAreaAcc = (AccessorViewArea1_21) viewArea;
        int sizeX = viewAreaAcc.fp2_getSectionGridSizeX();
        int sizeY = viewAreaAcc.fp2_getSectionGridSizeY();
        int sizeZ = viewAreaAcc.fp2_getSectionGridSizeZ();

        AccessorLevelRenderer1_21 lrAcc = (AccessorLevelRenderer1_21) levelRenderer;

        //figure out the chunk-coordinate (16-block) extents of the section grid
        int camSectionX = lrAcc.fp2_getLastCameraSectionX();
        int camSectionY = lrAcc.fp2_getLastCameraSectionY();
        int camSectionZ = lrAcc.fp2_getLastCameraSectionZ();

        int minChunkX = camSectionX - (sizeX >> 1);
        int maxChunkX = minChunkX + sizeX;
        int minChunkY = camSectionY - (sizeY >> 1);
        int maxChunkY = minChunkY + sizeY;
        int minChunkZ = camSectionZ - (sizeZ >> 1);
        int maxChunkZ = minChunkZ + sizeZ;

        int offsetChunkX = -minChunkX;
        int offsetChunkY = -minChunkY;
        int offsetChunkZ = -minChunkZ;
        int factorChunkX = sizeX;
        int factorChunkY = sizeY;
        int factorChunkZ = sizeZ;

        long[] srcFlags = new long[factorChunkX * factorChunkY * factorChunkZ];

        //pass 1: walk the SectionOcclusionGraph nodes for selected sections + their direction info
        SectionOcclusionGraph graph = lrAcc.fp2_getSectionOcclusionGraph();
        if (graph != null) {
            Collection<Object> nodes = SectionGraphAccess1_21.renderSections(graph);
            if (nodes != null) {
                for (Object node : nodes) {
                    AccessorSectionOcclusionGraphNode1_21 nodeAcc = (AccessorSectionOcclusionGraphNode1_21) node;
                    SectionRenderDispatcher.RenderSection section = nodeAcc.fp2_getSection();
                    BlockPos pos = section.getOrigin();
                    int cx = pos.getX() >> 4;
                    int cy = pos.getY() >> 4;
                    int cz = pos.getZ() >> 4;
                    if (cx < minChunkX || cx >= maxChunkX
                            || cy < minChunkY || cy >= maxChunkY
                            || cz < minChunkZ || cz >= maxChunkZ) {
                        continue;
                    }
                    int idx = ((cx + offsetChunkX) * factorChunkY + (cy + offsetChunkY)) * factorChunkZ + (cz + offsetChunkZ);
                    long flags = srcFlags[idx] | FLAG_SELECTED | FLAG_RENDERABLE;

                    //renderDirection flags: which neighbors this section will continue traversal to
                    byte directions = nodeAcc.fp2_getDirections();
                    int renderShift = SHIFT_RENDER_DIRECTIONS;
                    for (Direction d : DIRECTIONS) {
                        if ((directions & (1 << d.ordinal())) != 0) {
                            flags |= 1L << renderShift;
                        }
                        renderShift++;
                    }

                    //inFace: pick the lowest set bit of sourceDirections (1.21 uses a bitmask
                    //since multiple paths can reach the same node; transformFlags only takes
                    //one canonical direction)
                    byte sources = nodeAcc.fp2_getSourceDirections();
                    if (sources != 0) {
                        int firstSrc = Integer.numberOfTrailingZeros(sources & 0xFF);
                        if (firstSrc < DIRECTIONS.length) {
                            flags |= ((long) (firstSrc + 1)) << SHIFT_INFACE;
                        }
                    }

                    srcFlags[idx] = flags;
                }
            }
        }

        //fallback: also mark any sections in visibleSections that weren't in the graph nodes
        //(defensive — the two should match in practice, but be robust to graph not yet built)
        for (SectionRenderDispatcher.RenderSection section : lrAcc.fp2_getVisibleSections()) {
            BlockPos pos = section.getOrigin();
            int cx = pos.getX() >> 4;
            int cy = pos.getY() >> 4;
            int cz = pos.getZ() >> 4;
            if (cx < minChunkX || cx >= maxChunkX
                    || cy < minChunkY || cy >= maxChunkY
                    || cz < minChunkZ || cz >= maxChunkZ) {
                continue;
            }
            int idx = ((cx + offsetChunkX) * factorChunkY + (cy + offsetChunkY)) * factorChunkZ + (cz + offsetChunkZ);
            srcFlags[idx] |= FLAG_SELECTED | FLAG_RENDERABLE;
        }

        //pass 2: walk the full grid for FLAG_BAKED, FLAG_INFRUSTUM, and visibility flags
        for (SectionRenderDispatcher.RenderSection section : viewArea.sections) {
            if (section == null) {
                continue;
            }
            BlockPos pos = section.getOrigin();
            int cx = pos.getX() >> 4;
            int cy = pos.getY() >> 4;
            int cz = pos.getZ() >> 4;
            if (cx < minChunkX || cx >= maxChunkX
                    || cy < minChunkY || cy >= maxChunkY
                    || cz < minChunkZ || cz >= maxChunkZ) {
                continue;
            }
            int idx = ((cx + offsetChunkX) * factorChunkY + (cy + offsetChunkY)) * factorChunkZ + (cz + offsetChunkZ);

            long flags = srcFlags[idx];
            if (frustum.isVisible(section.getBoundingBox())) {
                flags |= FLAG_INFRUSTUM;
            }
            SectionRenderDispatcher.CompiledSection compiled = section.getCompiled();
            if (compiled != SectionRenderDispatcher.CompiledSection.UNCOMPILED) {
                flags |= FLAG_BAKED;
                flags |= visibilityFlags(compiled);
            }
            srcFlags[idx] = flags;
        }

        //pass 3: scan, transform, upload
        int scanMinX = 1, scanMaxX = factorChunkX - 1;
        int scanMinY = 1, scanMaxY = factorChunkY - 1;
        int scanMinZ = 1, scanMaxZ = factorChunkZ - 1;

        long addr = this.preTransformFlags(factorChunkX, factorChunkY, factorChunkZ,
                offsetChunkX, offsetChunkY, offsetChunkZ);
        transformFlags(scanMinX, scanMaxX, scanMinY, scanMaxY, scanMinZ, scanMaxZ,
                minChunkX, maxChunkX, minChunkY, maxChunkY, minChunkZ, maxChunkZ,
                factorChunkX, factorChunkY, factorChunkZ,
                offsetChunkX, offsetChunkY, offsetChunkZ,
                srcFlags, addr);
        this.postTransformFlags();
    }
}
//?}
