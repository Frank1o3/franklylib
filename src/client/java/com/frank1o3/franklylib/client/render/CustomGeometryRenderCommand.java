package com.frank1o3.franklylib.client.render;

import com.frank1o3.franklylib.Mesh;
import com.frank1o3.franklylib.MeshVertex;
import com.frank1o3.franklylib.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Submits a mesh's triangles as degenerate quads (Minecraft's entity vertex
 * consumers expect a QUADS-mode primitive, not a bare triangle list).
 *
 * <p>
 * <b>Double-sided submission:</b> for a procedural/deformed mesh (curved
 * surfaces built by generators or any
 * mesh whose winding isn't hand-verified per-triangle), it's easy for a
 * handful of triangles to end up with the "wrong" winding relative to the
 * camera. If the active {@code RenderType} culls backfaces, those triangles
 * are silently dropped — this shows up as a torn/checkerboard pattern of
 * missing triangles rather than an obvious visual glitch, which makes it easy
 * to miss.
 *
 * <p>
 * Rather than requiring every mesh generator to get winding perfectly
 * consistent, which manually reverses winding per face to
 * compensate), each triangle is submitted twice — once in its original
 * winding, once reversed — so it renders regardless of which side culling
 * considers "front". This roughly doubles vertex throughput for custom
 * geometry, which is an acceptable trade for a small number of attached
 * meshes (this is not used for full entity/world geometry).
 *
 * <p>
 * If you know a specific mesh's winding is already fully consistent and want
 * to avoid the throughput cost, submit through a {@code RenderType} that
 * disables culling (e.g. an equivalent of {@code entityCutoutNoCull}) instead
 * of relying on this double-submission — single-sided submission with a
 * no-cull render type is strictly cheaper. This class defaults to the safe
 * (double-sided) behaviour because it has no way to know the winding
 * guarantees of an arbitrary caller's mesh.
 */
public record CustomGeometryRenderCommand(
        Mesh mesh,
        Vec3[] deformedPositions,
        int light,
        int overlay,
        int color) implements SubmitNodeCollector.CustomGeometryRenderer {

    @Override
    public void render(PoseStack.Pose pose, VertexConsumer vertexConsumer) {
        if (mesh == null || mesh.vertices() == null || mesh.indices() == null) {
            return;
        }

        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        for (int i = 0; i < mesh.indices().length; i += 3) {
            int i0 = mesh.indices()[i];
            int i1 = mesh.indices()[i + 1];
            int i2 = mesh.indices()[i + 2];
            MeshVertex a = mesh.vertices()[i0];
            MeshVertex b = mesh.vertices()[i1];
            MeshVertex c = mesh.vertices()[i2];
            Vec3 pa = deformedPositions[i0];
            Vec3 pb = deformedPositions[i1];
            Vec3 pc = deformedPositions[i2];
            if (pa == null || pb == null || pc == null) {
                continue;
            }

            Vec3 ab = pb.subtract(pa);
            Vec3 ac = pc.subtract(pa);
            Vec3 faceNormal = ab.cross(ac).normalize();
            float defNx = matrix3f.m00() * faceNormal.x() + matrix3f.m10() * faceNormal.y() + matrix3f.m20() * faceNormal.z();
            float defNy = matrix3f.m01() * faceNormal.x() + matrix3f.m11() * faceNormal.y() + matrix3f.m21() * faceNormal.z();
            float defNz = matrix3f.m02() * faceNormal.x() + matrix3f.m12() * faceNormal.y() + matrix3f.m22() * faceNormal.z();

            float nax = (a.normal() != null && !a.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m00() * a.normal().x() + matrix3f.m10() * a.normal().y() + matrix3f.m20() * a.normal().z())
                    : defNx;
            float nay = (a.normal() != null && !a.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m01() * a.normal().x() + matrix3f.m11() * a.normal().y() + matrix3f.m21() * a.normal().z())
                    : defNy;
            float naz = (a.normal() != null && !a.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m02() * a.normal().x() + matrix3f.m12() * a.normal().y() + matrix3f.m22() * a.normal().z())
                    : defNz;

            float nbx = (b.normal() != null && !b.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m00() * b.normal().x() + matrix3f.m10() * b.normal().y() + matrix3f.m20() * b.normal().z())
                    : defNx;
            float nby = (b.normal() != null && !b.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m01() * b.normal().x() + matrix3f.m11() * b.normal().y() + matrix3f.m21() * b.normal().z())
                    : defNy;
            float nbz = (b.normal() != null && !b.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m02() * b.normal().x() + matrix3f.m12() * b.normal().y() + matrix3f.m22() * b.normal().z())
                    : defNz;

            float ncx = (c.normal() != null && !c.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m00() * c.normal().x() + matrix3f.m10() * c.normal().y() + matrix3f.m20() * c.normal().z())
                    : defNx;
            float ncy = (c.normal() != null && !c.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m01() * c.normal().x() + matrix3f.m11() * c.normal().y() + matrix3f.m21() * c.normal().z())
                    : defNy;
            float ncz = (c.normal() != null && !c.normal().equals(Vec3.ZERO))
                    ? (matrix3f.m02() * c.normal().x() + matrix3f.m12() * c.normal().y() + matrix3f.m22() * c.normal().z())
                    : defNz;

            // Entity render types use QUADS, not a triangle-list primitive.
            // Submit every triangle as a degenerate quad so the next triangle
            // always starts at a four-vertex boundary. Sending only three
            // vertices shifts the following primitive and causes missing faces
            // plus unrelated UV samples to bleed onto the mesh.

            // Front winding (a, b, c) — matches the mesh's authored index order.
            submitVertex(vertexConsumer, matrix4f, pa, a.u(), a.v(), nax, nay, naz);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), nbx, nby, nbz);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), ncx, ncy, ncz);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), ncx, ncy, ncz);

            // Reversed winding (a, c, b) with a flipped normal — guarantees this
            // triangle is still visible if the render type culls backfaces and
            // the authored winding happens to face away from the camera. This is
            // what actually fixes "randomly missing triangle" artifacts on
            // procedural/curved meshes, at the cost of ~2x vertices for custom
            // geometry submissions.
            submitVertex(vertexConsumer, matrix4f, pa, a.u(), a.v(), -nax, -nay, -naz);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), -ncx, -ncy, -ncz);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), -nbx, -nby, -nbz);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), -nbx, -nby, -nbz);
        }
    }

    private void submitVertex(VertexConsumer vertexConsumer, Matrix4f matrix4f, Vec3 position, float u, float v,
            float nx, float ny, float nz) {
        float px = position.x();
        float py = position.y();
        float pz = position.z();
        float tx = matrix4f.m00() * px + matrix4f.m10() * py + matrix4f.m20() * pz + matrix4f.m30();
        float ty = matrix4f.m01() * px + matrix4f.m11() * py + matrix4f.m21() * pz + matrix4f.m31();
        float tz = matrix4f.m02() * px + matrix4f.m12() * py + matrix4f.m22() * pz + matrix4f.m32();
        vertexConsumer.addVertex(tx, ty, tz, color, u, v, overlay, light, nx, ny, nz);
    }
}