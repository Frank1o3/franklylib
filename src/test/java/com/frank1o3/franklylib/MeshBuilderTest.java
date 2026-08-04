package com.frank1o3.franklylib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeshBuilderTest {
    @Test
    void boxProducesTrianglesAndValidIndices() {
        Mesh mesh = MeshBuilder.box(-1, -1, -1, 1, 1, 1);
        assertNotNull(mesh);
        assertEquals(8, mesh.vertices().length);
        assertTrue(mesh.indices().length % 3 == 0);
        assertTrue(mesh.indices().length > 0);
        for (int index : mesh.indices()) {
            assertTrue(index >= 0 && index < mesh.vertices().length, "Index out of range: " + index);
        }
    }

    @Test
    void mergeCombinesMeshes() {
        Mesh a = MeshBuilder.box(0, 0, 0, 1, 1, 1);
        Mesh b = MeshBuilder.box(1, 0, 0, 2, 1, 1);
        Mesh merged = MeshBuilder.merge(a, b);
        assertEquals(a.vertices().length + b.vertices().length, merged.vertices().length);
        assertTrue(merged.indices().length >= a.indices().length + b.indices().length);
    }
}
