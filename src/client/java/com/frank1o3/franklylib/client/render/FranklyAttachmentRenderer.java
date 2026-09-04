package com.frank1o3.franklylib.client.render;

import com.frank1o3.franklylib.Mesh;
import com.frank1o3.franklylib.MeshDeformer;
import com.frank1o3.franklylib.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;

public final class FranklyAttachmentRenderer {
    private FranklyAttachmentRenderer() {
    }

    public static <S extends HumanoidRenderState, M extends HumanoidModel<S>> void render(
            PoseStack poseStack,
            SubmitNodeCollector renderQueue,
            S state,
            M model,
            AttachmentPoint attachment,
            Mesh baseMesh,
            MeshDeformer deformer,
            RenderType renderType,
            int light,
            int overlay,
            int color,
            float partialTick) {
        if (attachment == null || baseMesh == null || renderType == null) {
            return;
        }
        poseStack.pushPose();
        ModelPart target = resolvePart(model, attachment.targetPart());
        if (target != null) {
            target.translateAndRotate(poseStack);
        }
        poseStack.translate(attachment.localOffset().x(), attachment.localOffset().y(),
                attachment.localOffset().z());
        if (attachment.localRotationEuler() != null && !attachment.localRotationEuler().equals(Vec3.ZERO)) {
            poseStack.mulPose(new org.joml.Quaternionf().rotationZYX(
                    attachment.localRotationEuler().z(),
                    attachment.localRotationEuler().y(),
                    attachment.localRotationEuler().x()));
        }
        poseStack.scale(attachment.localScale(), attachment.localScale(), attachment.localScale());
        Vec3[] positions = deformer != null ? deformer.deform(baseMesh, partialTick)
                : MeshDeformer.IDENTITY.deform(baseMesh, partialTick);
        if (positions != null) {
            renderQueue.submitCustomGeometry(poseStack, renderType,
                    new CustomGeometryRenderCommand(baseMesh, positions, light, overlay, color));
        }
        poseStack.popPose();
    }

    private static ModelPart resolvePart(HumanoidModel<?> model, String targetPart) {
        if (targetPart == null || targetPart.isBlank()) {
            return null;
        }
        return switch (targetPart) {
            case "head" -> model.getHead();
            case "body" -> model.body;
            case "right_arm" -> model.rightArm;
            case "left_arm" -> model.leftArm;
            case "right_leg" -> model.rightLeg;
            case "left_leg" -> model.leftLeg;
            default -> null;
        };
    }
}
