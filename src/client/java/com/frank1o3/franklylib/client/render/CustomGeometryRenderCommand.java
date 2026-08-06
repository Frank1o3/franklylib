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
            Vector3f transformedNormal = new Vector3f(faceNormal.x(), faceNormal.y(), faceNormal.z()).mul(matrix3f);

            submitVertex(vertexConsumer, matrix4f, pa, a.u(), a.v(), transformedNormal);
            submitVertex(vertexConsumer, matrix4f, pb, b.u(), b.v(), transformedNormal);
            submitVertex(vertexConsumer, matrix4f, pc, c.u(), c.v(), transformedNormal);
        }
    }

    private void submitVertex(VertexConsumer vertexConsumer, Matrix4f matrix4f, Vec3 position, float u, float v,
            Vector3f normal) {
        Vector4f transformed = new Vector4f(position.x(), position.y(), position.z(), 1.0F).mul(matrix4f);
        vertexConsumer.addVertex(transformed.x(), transformed.y(), transformed.z(), color, u, v, overlay, light,
                normal.x(), normal.y(), normal.z());
    }
}