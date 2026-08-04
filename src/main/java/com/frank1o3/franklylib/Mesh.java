package com.frank1o3.franklylib;

import java.util.Arrays;

public record Mesh(MeshVertex[] vertices, int[] indices) {
    public static Mesh of(MeshVertex[] vertices, int[] indices) {
        return new Mesh(vertices, indices);
    }

    public Mesh withComputedNormals() {
        if (indices.length % 3 != 0) {
            throw new IllegalArgumentException("Mesh indices must be a triangle list.");
        }

        float[] sumsX = new float[vertices.length];
        float[] sumsY = new float[vertices.length];
        float[] sumsZ = new float[vertices.length];
        int[] counts = new int[vertices.length];

        for (int i = 0; i < indices.length; i += 3) {
            int i0 = indices[i];
            int i1 = indices[i + 1];
            int i2 = indices[i + 2];
            MeshVertex a = vertices[i0];
            MeshVertex b = vertices[i1];
            MeshVertex c = vertices[i2];

            Vec3 ab = b.position().subtract(a.position());
            Vec3 ac = c.position().subtract(a.position());
            Vec3 normal = ab.cross(ac).normalize();
            if (normal.equals(Vec3.ZERO)) {
                continue;
            }

            for (int index : new int[] { i0, i1, i2 }) {
                sumsX[index] += normal.x();
                sumsY[index] += normal.y();
                sumsZ[index] += normal.z();
                counts[index]++;
            }
        }

        MeshVertex[] updated = new MeshVertex[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            Vec3 average = counts[i] == 0 ? Vec3.ZERO
                    : new Vec3(sumsX[i] / counts[i], sumsY[i] / counts[i], sumsZ[i] / counts[i]).normalize();
            updated[i] = new MeshVertex(vertices[i].position(), vertices[i].u(), vertices[i].v(), average);
        }
        return new Mesh(updated, Arrays.copyOf(indices, indices.length));
    }
}
