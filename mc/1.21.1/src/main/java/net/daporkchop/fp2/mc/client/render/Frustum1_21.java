package net.daporkchop.fp2.mc.client.render;

//? if neoforge {
import lombok.NonNull;
import net.daporkchop.fp2.core.client.IFrustum;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

// Wraps vanilla Frustum as IFrustum for AbstractFarRenderer.prepare(). configureClippingPlanes
// is stubbed (count=0) — disables GPU frustum culling shader path; CPU-side intersectsBB tests
// still cull non-visible tiles. Wire planes through later if GPU culling becomes a perf concern.
public final class Frustum1_21 implements IFrustum {
    private final Frustum delegate;

    public Frustum1_21(@NonNull Frustum delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean containsPoint(double x, double y, double z) {
        return this.delegate.isVisible(new AABB(x, y, z, x + 1.0e-3, y + 1.0e-3, z + 1.0e-3));
    }

    @Override
    public boolean intersectsBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.delegate.isVisible(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public void configureClippingPlanes(@NonNull ClippingPlanes clippingPlanes) {
        clippingPlanes.clippingPlaneCount(0);
    }

    @Override
    public void configureClippingPlanes(ShaderProgram.UniformSetter uniformSetter, UniformLocations locations) {
        uniformSetter.set1ui(locations.u_ClippingPlaneCount, 0);
    }
}
//?}
