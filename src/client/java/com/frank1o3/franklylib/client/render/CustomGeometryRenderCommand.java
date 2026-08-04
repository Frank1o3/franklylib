package com.frank1o3.franklylib.client.render;

import com.frank1o3.franklylib.Mesh;
import com.frank1o3.franklylib.MeshVertex;
import com.frank1o3.franklylib.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.ARGB;

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
        int alpha = ARGB.alpha(color);
        int red = ARGB.red(color);
        int green = ARGB.green(color);
        int blue = ARGB.blue(color);

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
            Vec3 normal = ab.cross(ac).normalize();
            vertexConsumer.setColor(red, green, blue, alpha);
            vertexConsumer.setLight(light);
            vertexConsumer.setOverlay(overlay);
            vertexConsumer.setNormal(pose, normal.x(), normal.y(), normal.z());
            submitVertex(pose, vertexConsumer, pa, a.u(), a.v());
            submitVertex(pose, vertexConsumer, pb, b.u(), b.v());
            submitVertex(pose, vertexConsumer, pc, c.u(), c.v());
        }
    }

    private static void submitVertex(PoseStack.Pose pose, VertexConsumer vertexConsumer, Vec3 position, float u,
            float v) {
        vertexConsumer.addVertex(pose, position.x(), position.y(), position.z());
        vertexConsumer.setUv(u, v);
    }
}
