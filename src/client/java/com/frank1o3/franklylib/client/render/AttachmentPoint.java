package com.frank1o3.franklylib.client.render;

import com.frank1o3.franklylib.Vec3;

public record AttachmentPoint(String targetPart, Vec3 localOffset, Vec3 localRotationEuler, float localScale) {
}
