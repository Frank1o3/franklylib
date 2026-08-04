# Mesh & Rendering API

FranklyLib's mesh types live in `com.frank1o3.franklylib` and are **common-side** —
no client dependencies — so they can be referenced from code that isn't client-only.
The rendering helpers that turn a mesh into submitted geometry live under
`com.frank1o3.franklylib.client.render` and are client-only.

A mesh is deliberately shape-agnostic: it's just a vertex list and a triangle index
list. Anything representable as triangles — a sphere, a hand-authored custom shape, a
flat UI-space fan — is a valid `Mesh`.

---

## Core types

### `Vec3`

A plain float 3-vector record with the arithmetic you'd expect:

```java
public record Vec3(float x, float y, float z) {
    public static final Vec3 ZERO;
    public Vec3 add(Vec3 other);
    public Vec3 subtract(Vec3 other);
    public Vec3 scale(float factor);
    public Vec3 normalize();      // returns ZERO for a zero-length vector
    public float dot(Vec3 other);
    public Vec3 cross(Vec3 other);
}
```

### `MeshVertex`

```java
public record MeshVertex(Vec3 position, float u, float v, Vec3 normal) {
    public MeshVertex(Vec3 position, float u, float v); // normal defaults to Vec3.ZERO
}
```

### `Mesh`

```java
public record Mesh(MeshVertex[] vertices, int[] indices) {
    public static Mesh of(MeshVertex[] vertices, int[] indices);
    public Mesh withComputedNormals(); // flat-shaded, per-triangle normals averaged into shared vertices
}
```

`indices` must be a flat triangle list (`indices.length % 3 == 0`). You can construct a
`Mesh` directly from your own vertex/index arrays — the generators below are just
convenience for common shapes, not the only way to get a `Mesh`.

`withComputedNormals()` throws `IllegalArgumentException` if `indices.length` isn't a
multiple of 3. Degenerate triangles (zero-area) are skipped when accumulating normals.

---

## MeshBuilder — primitive generators

Static factory methods on `com.frank1o3.franklylib.MeshBuilder`:

| Method | Produces |
| --- | --- |
| `plane(origin, uAxis, vAxis, width, height, subdivisionsU, subdivisionsV)` | A subdivided flat quad |
| `triangleFan(center, rimPoints)` | A fan of triangles from a center point to a rim (≥ 3 rim points required) |
| `box(minX, minY, minZ, maxX, maxY, maxZ)` | An 8-vertex, 12-triangle cuboid |
| `subdividedBox(minX, minY, minZ, maxX, maxY, maxZ, subdivisionsPerFace)` | A cuboid with a subdivided grid per face |
| `uvSphere(center, radius, rings, segments)` | A UV sphere |
| `cylinder(base, axis, radius, height, radialSegments, capped)` | A cylinder, optionally capped |
| `cone(base, axis, radius, height, radialSegments, capped)` | A cone, optionally capped |
| `torus(center, axis, majorRadius, minorRadius, majorSegments, minorSegments)` | A torus |
| `merge(Mesh...)` | Concatenates multiple meshes into one, fixing up index offsets |

All generators produce UVs in `[0, 1]` over their own parametrization and CCW winding
by convention. `merge()` lets you build a compound shape (e.g. a cylinder plus two cone
caps) and treat it as a single `Mesh` for deformation and rendering purposes.

```java
Mesh sphere = MeshBuilder.uvSphere(Vec3.ZERO, 1.0f, 12, 12).withComputedNormals();

Mesh capsule = MeshBuilder.merge(
        MeshBuilder.cylinder(new Vec3(0, 0, 0), new Vec3(0, 1, 0), 0.5f, 1.0f, 12, false),
        MeshBuilder.uvSphere(new Vec3(0, 1, 0), 0.5f, 8, 12),
        MeshBuilder.uvSphere(new Vec3(0, 0, 0), 0.5f, 8, 12)
);
```

---

## MeshDeformer

A plain, shape-agnostic per-frame deformation contract. FranklyLib has no opinion on
*why* vertices move — only on the shape of the callback:

```java
@FunctionalInterface
public interface MeshDeformer {
    Vec3[] deform(Mesh baseMesh, float partialTick);

    MeshDeformer IDENTITY; // returns each vertex's own position, unmodified
}
```

Implement this for physics-driven jiggle, a sine-wave sway, skeletal-style weighting,
or anything else — `MeshDeformer.IDENTITY` covers "just render the mesh as-is."

---

## Rendering

### CustomGeometryRenderCommand (entity-independent)

The lowest-level rendering building block: a
`SubmitNodeCollector.CustomGeometryRenderer` that submits a mesh's triangles given
already-deformed vertex positions, with flat per-triangle normals computed at submit
time.

```java
public record CustomGeometryRenderCommand(
        Mesh mesh,
        Vec3[] deformedPositions,
        int light,
        int overlay,
        int color
) implements SubmitNodeCollector.CustomGeometryRenderer { ... }
```

Use this directly for free-floating geometry — push whatever `PoseStack` transform you
want (world position, GUI-space, wherever), submit a `CustomGeometryRenderCommand`, pop.
No entity involved:

```java
poseStack.pushPose();
poseStack.translate(x, y, z);
Vec3[] positions = MeshDeformer.IDENTITY.deform(mesh, partialTick);
renderQueue.submitCustomGeometry(poseStack, renderType,
        new CustomGeometryRenderCommand(mesh, positions, light, overlay, color));
poseStack.popPose();
```

Vertices with a `null` deformed position are skipped (their triangle is not
submitted), so a deformer can selectively "hide" vertices by omitting a value.

### AttachmentPoint + FranklyAttachmentRenderer (entity attachment)

For binding a mesh to a point on a living entity's humanoid model:

```java
public record AttachmentPoint(
        String targetPart,          // "head" | "body" | "right_arm" | "left_arm" | "right_leg" | "left_leg"
        Vec3 localOffset,
        Vec3 localRotationEuler,
        float localScale
) {}
```

```java
public final class FranklyAttachmentRenderer {
    public static <S extends HumanoidRenderState, M extends HumanoidModel<S>> void render(
            PoseStack poseStack,
            SubmitNodeCollector renderQueue,
            S state,
            M model,
            AttachmentPoint attachment,
            Mesh baseMesh,
            MeshDeformer deformer,
            RenderType renderType,
            int light, int overlay, int color,
            float partialTick);
}
```

Resolves `targetPart` against the model's known humanoid part names, applies that
part's current pose transform (so the attachment follows limb swing / head look for
free), then applies the attachment's own local offset/rotation/scale before deforming
and submitting via `CustomGeometryRenderCommand`. If `attachment` or `baseMesh` is
`null`, or `targetPart` doesn't match a known name, the call is a no-op for the part
resolution step (rendering still proceeds with an unrotated pose in the null-target
case).

```java
FranklyAttachmentRenderer.render(
        poseStack, renderQueue, renderState, model,
        new AttachmentPoint("head", new Vec3(0, 0.2f, 0), Vec3.ZERO, 1.0f),
        antennaMesh, MeshDeformer.IDENTITY, RenderType.entityCutout(texture),
        light, overlay, 0xFFFFFFFF, partialTick);
```

This is a static helper rather than a `RenderLayer` itself, so it can be called from
inside your own mod's `RenderLayer` without fighting layer-registration order — your
mod owns *when* this runs, FranklyLib only owns *how* the mesh gets from "deformed
vertex list" to "submitted triangles positioned relative to a body part."

> **Currently supported model shape:** `FranklyAttachmentRenderer` only knows how to
> resolve `targetPart` against `HumanoidModel`'s six standard parts. There's no
> caller-supplied lookup function for non-humanoid models yet, despite that being
> mentioned as an option in the original design notes — if you need to attach to a
> non-humanoid model, you'll need to extend this yourself for now.

### GUI-space mesh rendering

Not currently implemented. Rendering a `Mesh` inside a 2D screen (e.g. a rotating
preview built from geometry instead of a texture) would reuse the same
`CustomGeometryRenderCommand` with a `PoseStack` built from screen coordinates instead
of an entity's world transform — but the exact projection setup depends on how "3D in
a 2D screen" is composed in the consuming mod, so it's left out of this version of the
library.
