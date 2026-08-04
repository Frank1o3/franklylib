package com.frank1o3.franklylib;

@FunctionalInterface
public interface MeshDeformer {
    Vec3[] deform(Mesh baseMesh, float partialTick);

    MeshDeformer IDENTITY = (mesh, partialTick) -> {
        Vec3[] out = new Vec3[mesh.vertices().length];
        for (int i = 0; i < out.length; i++) {
            out[i] = mesh.vertices()[i].position();
        }
        return out;
    };
}
