package com.frank1o3.franklylib.client.render;

import com.frank1o3.franklylib.Mesh;
import com.frank1o3.franklylib.MeshVertex;
import com.frank1o3.franklylib.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
            Vector3f defaultNormal = new Vector3f(faceNormal.x(), faceNormal.y(), faceNormal.z()).mul(matrix3f);

            Vector3f na = a.normal() != null && !a.normal().equals(Vec3.ZERO)
                    ? new Vector3f(a.normal().x(), a.normal().y(), a.normal().z()).mul(matrix3f)
                    : defaultNormal;
            Vector3f nb = b.normal() != null && !b.normal().equals(Vec3.ZERO)
                    ? new Vector3f(b.normal().x(), b.normal().y(), b.normal().z()).mul(matrix3f)
                    : defaultNormal;
            Vector3f nc = c.normal() != null && !c.normal().equals(Vec3.ZERO)
                    ? new Vector3f(c.normal().x(), c.normal().y(), c.normal().z()).mul(matrix3f)
                    : defaultNormal;

            Vector3f rna = new Vector3f(na).negate();
            Vector3f rnb = new Vector3f(nb).negate();
            Vector3f rnc = new Vector3f(nc).negate();

            // Entity render types use QUADS, not a triangle-list primitive.
            // Submit every triangle as a degenerate quad so the next triangle
            // always starts at a four-vertex boundary. Sending only three
            // vertices shifts the following primitive and causes missing faces
            // plus unrelated UV samples to bleed onto the mesh.

            // Front winding (a, b, c) — matches the mesh's authored index order.
            submitVertex(vertexConsumer, matrix4f, pa, a.u(), a.v(), na);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), nb);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), nc);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), nc);

            // Reversed winding (a, c, b) with a flipped normal — guarantees this
            // triangle is still visible if the render type culls backfaces and
            // the authored winding happens to face away from the camera. This is
            // what actually fixes "randomly missing triangle" artifacts on
            // procedural/curved meshes, at the cost of ~2x vertices for custom
            // geometry submissions.
            submitVertex(vertexConsumer, matrix4f, pa, a.u(), a.v(), rna);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), rnc);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), rnb);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), rnb);
        }
    }

    private void submitVertex(VertexConsumer vertexConsumer, Matrix4f matrix4f, Vec3 position, float u, float v,
            Vector3f normal) {
        Vector4f transformed = new Vector4f(position.x(), position.y(), position.z(), 1.0F).mul(matrix4f);
        vertexConsumer.addVertex(transformed.x(), transformed.y(), transformed.z(), color, u, v, overlay, light,
                normal.x(), normal.y(), normal.z());
    }
}