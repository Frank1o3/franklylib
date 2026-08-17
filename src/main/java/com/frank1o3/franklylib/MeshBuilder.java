package com.frank1o3.franklylib;

import java.util.ArrayList;
import java.util.List;

public final class MeshBuilder {
    private MeshBuilder() {
    }

    public static Mesh plane(Vec3 origin, Vec3 uAxis, Vec3 vAxis, float width, float height, int subdivisionsU,
            int subdivisionsV) {
        int uSteps = Math.max(1, subdivisionsU + 1);
        int vSteps = Math.max(1, subdivisionsV + 1);
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        Vec3 u = uAxis.normalize().scale(width / Math.max(1, subdivisionsU));
        Vec3 v = vAxis.normalize().scale(height / Math.max(1, subdivisionsV));
        Vec3 base = origin;

        for (int y = 0; y < vSteps; y++) {
            for (int x = 0; x < uSteps; x++) {
                Vec3 position = base.add(u.scale(x)).add(v.scale(y));
                float uCoord = (float) x / Math.max(1, subdivisionsU);
                float vCoord = (float) y / Math.max(1, subdivisionsV);
                vertices.add(new MeshVertex(position, uCoord, vCoord));
            }
        }

        for (int y = 0; y < vSteps - 1; y++) {
            for (int x = 0; x < uSteps - 1; x++) {
                int a = y * uSteps + x;
                int b = y * uSteps + x + 1;
                int c = (y + 1) * uSteps + x;
                int d = (y + 1) * uSteps + x + 1;
                indices.add(a);
                indices.add(b);
                indices.add(c);
                indices.add(b);
                indices.add(d);
                indices.add(c);
            }
        }

        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Generates an open triangle fan from a central point and a sequence of rim points.
     * Note: This produces an open fan (does not generate a closing triangle between the last
     * and first rim points). If a closed fan is desired, include the first rim point at the end.
     */
    public static Mesh triangleFan(Vec3 center, List<Vec3> rimPoints) {
        if (rimPoints.size() < 3) {
            throw new IllegalArgumentException("triangleFan requires at least 3 rim points");
        }
        MeshVertex[] vertices = new MeshVertex[rimPoints.size() + 1];
        vertices[0] = new MeshVertex(center, 0.5f, 0.5f);
        int[] indices = new int[(rimPoints.size() - 1) * 3];
        for (int i = 0; i < rimPoints.size(); i++) {
            Vec3 point = rimPoints.get(i);
            vertices[i + 1] = new MeshVertex(point, (float) i / rimPoints.size(), 1.0f);
            if (i >= 1) {
                indices[(i - 1) * 3] = 0;
                indices[(i - 1) * 3 + 1] = i;
                indices[(i - 1) * 3 + 2] = i + 1;
            }
        }
        return Mesh.of(vertices, indices);
    }

    public static Mesh box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        MeshVertex[] vertices = new MeshVertex[] {
                new MeshVertex(new Vec3(minX, minY, minZ), 0f, 0f),
                new MeshVertex(new Vec3(maxX, minY, minZ), 1f, 0f),
                new MeshVertex(new Vec3(maxX, maxY, minZ), 1f, 1f),
                new MeshVertex(new Vec3(minX, maxY, minZ), 0f, 1f),
                new MeshVertex(new Vec3(minX, minY, maxZ), 0f, 0f),
                new MeshVertex(new Vec3(maxX, minY, maxZ), 1f, 0f),
                new MeshVertex(new Vec3(maxX, maxY, maxZ), 1f, 1f),
                new MeshVertex(new Vec3(minX, maxY, maxZ), 0f, 1f)
        };

        int[] indices = new int[] {
                0, 1, 2,
                0, 2, 3,
                4, 6, 5,
                4, 7, 6,
                0, 3, 7,
                0, 7, 4,
                1, 5, 6,
                1, 6, 2,
                0, 4, 5,
                0, 5, 1,
                3, 2, 6,
                3, 6, 7
        };
        return Mesh.of(vertices, indices);
    }

    public static Mesh subdividedBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
            int subdivisionsPerFace) {
        int steps = Math.max(1, subdivisionsPerFace);
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int face = 0; face < 6; face++) {
            int base = vertices.size();
            int uSteps = steps + 1;
            int vSteps = steps + 1;
            for (int y = 0; y < vSteps; y++) {
                for (int x = 0; x < uSteps; x++) {
                    float u = (float) x / steps;
                    float v = (float) y / steps;
                    Vec3 pos = switch (face) {
                        case 0 -> new Vec3(minX + (maxX - minX) * u, minY + (maxY - minY) * v, minZ);
                        case 1 -> new Vec3(maxX, minY + (maxY - minY) * v, minZ + (maxZ - minZ) * u);
                        case 2 -> new Vec3(minX + (maxX - minX) * u, maxY, minZ + (maxZ - minZ) * v);
                        case 3 -> new Vec3(minX, minY + (maxY - minY) * v, minZ + (maxZ - minZ) * u);
                        case 4 -> new Vec3(minX + (maxX - minX) * u, minY, minZ + (maxZ - minZ) * v);
                        default -> new Vec3(minX + (maxX - minX) * u, minY + (maxY - minY) * v, maxZ);
                    };
                    vertices.add(new MeshVertex(pos, u, v));
                }
            }
            for (int y = 0; y < steps; y++) {
                for (int x = 0; x < steps; x++) {
                    int a = base + y * uSteps + x;
                    int b = base + y * uSteps + x + 1;
                    int c = base + (y + 1) * uSteps + x;
                    int d = base + (y + 1) * uSteps + x + 1;
                    indices.add(a);
                    indices.add(b);
                    indices.add(c);
                    indices.add(b);
                    indices.add(d);
                    indices.add(c);
                }
            }
        }
        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }

    public static Mesh uvSphere(Vec3 center, float radius, int rings, int segments) {
        int ringCount = Math.max(3, rings);
        int segCount = Math.max(3, segments);
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int ring = 0; ring <= ringCount; ring++) {
            float v = (float) ring / ringCount;
            float theta = (float) (Math.PI * v);
            float sinTheta = (float) Math.sin(theta);
            for (int segment = 0; segment <= segCount; segment++) {
                float u = (float) segment / segCount;
                float phi = (float) (2.0 * Math.PI * u);
                float x = center.x() + radius * (float) Math.sin(phi) * sinTheta;
                float y = center.y() + radius * (float) Math.cos(theta);
                float z = center.z() + radius * (float) Math.cos(phi) * sinTheta;
                vertices.add(new MeshVertex(new Vec3(x, y, z), u, v));
            }
        }
        for (int ring = 0; ring < ringCount; ring++) {
            for (int segment = 0; segment < segCount; segment++) {
                int a = ring * (segCount + 1) + segment;
                int b = a + 1;
                int c = (ring + 1) * (segCount + 1) + segment;
                int d = c + 1;
                indices.add(a);
                indices.add(b);
                indices.add(c);
                indices.add(b);
                indices.add(d);
                indices.add(c);
            }
        }
        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }

    public static Mesh cylinder(Vec3 base, Vec3 axis, float radius, float height, int radialSegments, boolean capped) {
        Vec3 normalizedAxis = axis.normalize();
        Vec3 side = normalizedAxis.cross(new Vec3(0f, 1f, 0f));
        if (side.dot(side) < 0.001f) {
            side = normalizedAxis.cross(new Vec3(1f, 0f, 0f));
        }
        Vec3 u = side.normalize();
        Vec3 v = normalizedAxis.cross(u).normalize();
        Vec3 top = base.add(normalizedAxis.scale(height));
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int segments = Math.max(3, radialSegments);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            Vec3 point = base.add(u.scale((float) Math.cos(angle) * radius))
                    .add(v.scale((float) Math.sin(angle) * radius));
            vertices.add(new MeshVertex(point, (float) i / segments, 0f));
            Vec3 topPoint = top.add(u.scale((float) Math.cos(angle) * radius))
                    .add(v.scale((float) Math.sin(angle) * radius));
            vertices.add(new MeshVertex(topPoint, (float) i / segments, 1f));
        }
        for (int i = 0; i < segments; i++) {
            int a = i * 2;
            int b = a + 1;
            int c = a + 2;
            int d = c + 1;
            indices.add(a);
            indices.add(c);
            indices.add(b);
            indices.add(b);
            indices.add(c);
            indices.add(d);
        }
        if (capped) {
            MeshVertex centerBottom = new MeshVertex(base, 0.5f, 0.5f);
            MeshVertex centerTop = new MeshVertex(top, 0.5f, 0.5f);
            vertices.add(centerBottom);
            vertices.add(centerTop);
            int bottomCenter = vertices.size() - 2;
            int topCenter = vertices.size() - 1;
            for (int i = 0; i < segments; i++) {
                int a = i * 2;
                int b = i * 2 + 2;
                int c = bottomCenter;
                indices.add(a);
                indices.add(b);
                indices.add(c);
                int d = i * 2 + 1;
                int e = i * 2 + 3;
                indices.add(e);
                indices.add(d);
                indices.add(topCenter);
            }
        }
        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }

    public static Mesh cone(Vec3 base, Vec3 axis, float radius, float height, int radialSegments, boolean capped) {
        Vec3 normalizedAxis = axis.normalize();
        Vec3 side = normalizedAxis.cross(new Vec3(0f, 1f, 0f));
        if (side.dot(side) < 0.001f) {
            side = normalizedAxis.cross(new Vec3(1f, 0f, 0f));
        }
        Vec3 u = side.normalize();
        Vec3 v = normalizedAxis.cross(u).normalize();
        Vec3 tip = base.add(normalizedAxis.scale(height));
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int segments = Math.max(3, radialSegments);
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0 * Math.PI * i / segments);
            Vec3 ringPoint = base.add(u.scale((float) Math.cos(angle) * radius))
                    .add(v.scale((float) Math.sin(angle) * radius));
            vertices.add(new MeshVertex(ringPoint, (float) i / segments, 0f));
        }
        vertices.add(new MeshVertex(tip, 0.5f, 1f));
        for (int i = 0; i < segments; i++) {
            int a = i;
            int b = i + 1;
            int c = segments + 1;
            indices.add(a);
            indices.add(b);
            indices.add(c);
        }
        if (capped) {
            MeshVertex center = new MeshVertex(base, 0.5f, 0.5f);
            vertices.add(center);
            int capCenter = vertices.size() - 1;
            for (int i = 0; i < segments; i++) {
                int a = i;
                int b = i + 1;
                int c = capCenter;
                indices.add(a);
                indices.add(c);
                indices.add(b);
            }
        }
        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }

    public static Mesh torus(Vec3 center, Vec3 axis, float majorRadius, float minorRadius, int majorSegments,
            int minorSegments) {
        int major = Math.max(3, majorSegments);
        int minor = Math.max(3, minorSegments);
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        Vec3 normalizedAxis = axis.normalize();
        Vec3 side = normalizedAxis.cross(new Vec3(0f, 1f, 0f));
        if (side.dot(side) < 0.001f) {
            side = normalizedAxis.cross(new Vec3(1f, 0f, 0f));
        }
        Vec3 u = side.normalize();
        Vec3 v = normalizedAxis.cross(u).normalize();
        for (int i = 0; i <= major; i++) {
            float majorAngle = (float) (2.0 * Math.PI * i / major);
            float cosMajor = (float) Math.cos(majorAngle);
            float sinMajor = (float) Math.sin(majorAngle);
            Vec3 radial = u.scale(cosMajor).add(v.scale(sinMajor));
            Vec3 ringCenter = center.add(radial.scale(majorRadius));
            for (int j = 0; j <= minor; j++) {
                float minorAngle = (float) (2.0 * Math.PI * j / minor);
                float cosMinor = (float) Math.cos(minorAngle);
                float sinMinor = (float) Math.sin(minorAngle);
                Vec3 point = ringCenter.add(radial.scale(cosMinor * minorRadius))
                        .add(normalizedAxis.scale(sinMinor * minorRadius));
                vertices.add(new MeshVertex(point, (float) i / major, (float) j / minor));
            }
        }
        for (int i = 0; i < major; i++) {
            for (int j = 0; j < minor; j++) {
                int a = i * (minor + 1) + j;
                int b = a + 1;
                int c = (i + 1) * (minor + 1) + j;
                int d = c + 1;
                indices.add(a);
                indices.add(b);
                indices.add(c);
                indices.add(b);
                indices.add(d);
                indices.add(c);
            }
        }
        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }

    public static Mesh merge(Mesh... meshes) {
        if (meshes.length == 0) {
            return Mesh.of(new MeshVertex[0], new int[0]);
        }
        List<MeshVertex> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int offset = 0;
        for (Mesh mesh : meshes) {
            for (MeshVertex vertex : mesh.vertices()) {
                vertices.add(vertex);
            }
            for (int index : mesh.indices()) {
                indices.add(index + offset);
            }
            offset += mesh.vertices().length;
        }
        return Mesh.of(vertices.toArray(new MeshVertex[0]), indices.stream().mapToInt(Integer::intValue).toArray());
    }
}
