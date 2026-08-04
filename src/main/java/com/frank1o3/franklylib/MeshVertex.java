package com.frank1o3.franklylib;

public record MeshVertex(Vec3 position, float u, float v, Vec3 normal) {
    public MeshVertex(Vec3 position, float u, float v) {
        this(position, u, v, Vec3.ZERO);
    }
}
