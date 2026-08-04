# FranklyLib — Library Mod Design Specification

**Purpose of this document:** design spec for a standalone, publishable Fabric library
mod that provides (1) a small set of good-looking custom GUI widgets and (2) a general
purpose API for building and rendering arbitrary triangle meshes — including but not
limited to attaching them to living entities. This mod has **no gameplay of its own**
and ships no networking code; it's a toolkit other mods depend on. It is written to be
generically useful to any mod needing custom UI or custom 3D geometry, not tied to any
particular use case.

Do not port code from any other mod. The only pre-existing code to reuse is your four
existing GUI files (Section 2.1) — everything else described here is new, original
code written to this spec.

---

## 0. Identity

| Field | Value |
| --- | --- |
| Mod name | FranklyLib |
| Mod ID | `franklylib` |
| Root Java package | `com.frank1o3.franklylib` |
| Loader | Fabric |
| Minecraft version | match your current target (26.2 / Java 25) |
| Source layout | `splitEnvironmentSourceSets()` — this mod is almost entirely client-side, but keep a `main` source set for the handful of common-side pure-data types (mesh descriptors) so other mods can reference them from common code without pulling in client classes. |
| Networking | **None.** No packets, no codecs for network transport. Consumers own their own sync entirely. |
| Scope boundary | UI widgets + mesh building/rendering utilities only. No config persistence, no entity data caching, no physics, no gameplay logic of any kind. |

Package roots:

- `com.frank1o3.franklylib` — common-side pure data (mesh descriptors, math helpers)
- `com.frank1o3.franklylib.client.gui` — widgets and screens
- `com.frank1o3.franklylib.client.render` — mesh building + generic mesh rendering

---

## 1. What This Library Is

A dependency-free toolkit for two unrelated but commonly-paired needs in Fabric mods:

1. A cohesive set of custom-drawn GUI widgets (buttons, sliders, dropdowns, text boxes,
   checkboxes, tabs, scrollable lists) that all share one visual language, so a mod using
   this library doesn't end up mixing vanilla's beveled widgets with ad-hoc custom ones.
2. A **general-purpose arbitrary-mesh construction and rendering API** — build any
   triangle mesh (procedural primitives or fully custom vertex/index data), optionally
   deform its vertices per-frame via a plug-in callback, and render it either as free-
   floating 3D geometry or attached to a point on a living entity's model. This is *not*
   a "cuboid deformation" system — cuboids are just one of several built-in primitive
   generators. The mesh type itself is a plain vertex/index list and can represent
   anything: a sphere, a custom hand-authored shape, a shape generated from a formula, a
   flat UI-space triangle fan, whatever the consumer needs.

## 2. What This Library Is *Not*

To keep scope honest and keep this reusable across unrelated mods:

- No physics simulation of any kind. Vertex deformation is a pure `(Mesh, float) -> Vec3[]`
  callback the consumer supplies; the library has no opinion on what drives it (could be
  physics, could be a sine-wave animation, could be nothing at all).
- No per-entity or per-player config storage/caching.
- No networking/sync.
- No assumptions about *what* a mesh represents or *where* it's rendered. Body
  attachments, floating decorative geometry, in-world markers, custom particle-like
  effects, and 3D elements inside a GUI screen are all equally valid, equally
  unprivileged uses of the same mesh/render API.

---

## 3. GUI Toolkit (`com.frank1o3.franklylib.client.gui`)

### 3.1 Foundation — reused verbatim from your existing files

Copy these in unmodified (package declaration updated to
`com.frank1o3.franklylib.client.gui`), and treat them as the base every other widget in
this library builds on top of / matches the visual language of:

- `ScaleSlider.java` → **`FranklySlider`**.
- `ScaleButton.java` → **`FranklyButton`**.
- `ScaleGuiUtils.java` → **`FranklyGuiUtils`**. Keep `drawFittedText` and
  `drawScaledEntityPreview` as-is; this becomes the shared text/entity-preview toolbox
  for every widget below.
- `BaseScaleScreen.java` → **`BaseFranklyScreen`**. Keep the panel/border/title chrome
  exactly as-is; every new screen type in this library extends it.

`FranklyGuiUtils` currently depends on an `InventoryScreenAccessor` mixin — give
FranklyLib its own copy of that one-method accessor under
`com.frank1o3.franklylib.client.mixin.accessors`, and register it in this mod's own
mixin config, so any mod depending on FranklyLib gets `drawScaledEntityPreview` for free
without needing its own copy of that mixin.

### 3.2 New widgets to add

These are the gaps between what your four files currently cover (slider, flat button,
scrollable/centered text, entity preview) and a complete "nice-looking custom UI" kit.
Build each to visually match `FranklyButton`/`FranklySlider`'s flat, semi-transparent,
rounded-nothing aesthetic (solid fill + hover-lighten + disabled-darken + optional
border) so a screen built from a mix of these widgets looks like one coherent design
system, not mismatched vanilla + custom pieces.

#### `FranklyDropdown<T>`

- Closed state renders like a `FranklyButton` showing the current selection + a small
  indicator glyph (▾).
- Click opens a floating option list (a `FranklyScrollList`, see below) anchored below
  (or above, if it'd overflow the screen) the dropdown's bounds, rendered as a
  *top-level overlay* — needs to draw after all other widgets and intercept the next
  click regardless of which widget technically "owns" screen space. Simplest correct
  approach: the owning `BaseFranklyScreen` holds an `Optional<OpenDropdownState>` and
  the screen itself renders/routes clicks for the open dropdown last, closing it on any
  click outside its bounds or on selection.
- Generic over `T` with a `Function<T, Component>` label mapper, so it works for enum
  values, arbitrary registry entries, plain strings, whatever a consumer needs.
- Builder pattern matching `FranklyButton.Builder`'s style (`.bounds(...)`, `.options(List<T>)`,
  `.current(T)`, `.labelMapper(Function<T,Component>)`, `.onSelect(Consumer<T>)`).

#### `FranklyTextBox`

- Single-line text input: blinking caret, selection highlight, horizontal scroll when
  content overflows. Use a caret-follow scroll (clamp/scroll-to-caret), not a marquee —
  marquee is the wrong behavior for a field the user is actively typing in.
- Optional `Predicate<String>` filter (e.g. digits-only) and optional max length.
- Builder: `.bounds(...)`, `.initialValue(String)`, `.filter(Predicate<String>)`,
  `.maxLength(int)`, `.onChanged(Consumer<String>)`, `.onSubmit(Consumer<String>)` (Enter
  key).

#### `FranklyCheckbox`

- Small square indicator + label text to the right, toggles a boolean, same
  flat-fill/hover/disabled color language as `FranklyButton`.
- Builder: `.bounds(...)`, `.label(Component)`, `.checked(boolean)`, `.onToggle(Consumer<Boolean>)`.

#### `FranklyTabBar<T>`

- Row of `FranklyButton`-styled segments, exactly one "active" (non-clickable, visually
  distinct) at a time — a single reusable widget for tab-switching UI instead of every
  screen hand-rolling its own tab buttons + enum + rebuild dance.
- Builder: `.bounds(x, y, totalWidth, height)`, `.tabs(List<T>)`, `.labelMapper(...)`,
  `.current(T)`, `.onSelect(Consumer<T>)`. Internally lays out N equal-width segments
  across `totalWidth`.

#### `FranklyScrollList<T>`

- Vertical list of rows within a fixed-height viewport, mouse-wheel + optional drag
  scrollbar, each row rendered via a consumer-supplied `RowRenderer<T>` callback (so it
  can host anything: plain text rows, a row with a button, an entity-preview tile,
  whatever). This is the backing widget for `FranklyDropdown`'s option list, and is also
  generally useful standalone.
- Builder: `.bounds(...)`, `.items(List<T>)`, `.rowHeight(int)`, `.rowRenderer(RowRenderer<T>)`,
  `.onClickRow(Consumer<T>)`.

#### `FranklyTooltipUtils`

- Small static helper (separate from `FranklyGuiUtils` to keep concerns split) for
  drawing a themed tooltip box matching this library's visual language, for widgets that
  want a custom tooltip instead of vanilla's `Tooltip.create(...)` styling. Optional —
  vanilla tooltips via `AbstractWidget#setTooltip` are perfectly fine to keep using for
  most cases; only build this if you specifically want tooltips visually consistent with
  the flat widget theme.

### 3.3 Screen-level helper: pagination

Add a small `PaginatedContent<T>` helper (not a widget, a plain utility class) that
takes a full `List<T>`, a page size, and a current page index, and returns the visible
slice + whether prev/next are available — generalizing manual
`startIndex`/`endIndex`/`getTotalPages()` bookkeeping so `BaseFranklyScreen` subclasses
don't each reimplement it for grid/paged content.

---

## 4. Mesh Construction (`com.frank1o3.franklylib.mesh`, common-side)

The mesh type is deliberately shape-agnostic — it's just vertices and triangle indices.
Anything renderable as triangles is representable, from a single quad to a fully custom
hand-authored model.

### 4.1 Core types

```java
public record MeshVertex(Vec3 position, float u, float v, Vec3 normal) {
    public MeshVertex(Vec3 position, float u, float v) {
        this(position, u, v, Vec3.ZERO); // normal computed later if left zero
    }
}

public record Mesh(MeshVertex[] vertices, int[] indices) {
    // indices.length % 3 == 0, triangle list, CCW winding by convention

    public static Mesh of(MeshVertex[] vertices, int[] indices) {
        return new Mesh(vertices, indices);
    }

    public Mesh withComputedNormals() {
        // flat-shaded: derive each triangle's normal, average into shared vertices
    }
}
```

Any mod can construct a `Mesh` directly from its own vertex/index arrays — this is the
"fully custom shape" escape hatch that doesn't depend on any of the primitive generators
below. The generators in 4.2 exist purely for convenience on common shapes; they are not
the only way to get a `Mesh`.

### 4.2 `MeshBuilder` — primitive generators

Static factory methods covering the common procedural shapes a mod is likely to want,
explicitly **not limited to cuboids**:

```java
public final class MeshBuilder {

    // --- Flat / planar ---
    public static Mesh plane(Vec3 origin, Vec3 uAxis, Vec3 vAxis,
                              float width, float height, int subdivisionsU, int subdivisionsV);

    public static Mesh triangleFan(Vec3 center, List<Vec3> rimPoints);

    // --- Cuboid ---
    public static Mesh box(float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ);              // 8 verts, 12 tris

    public static Mesh subdividedBox(float minX, float minY, float minZ,
                                      float maxX, float maxY, float maxZ,
                                      int subdivisionsPerFace);                // grid per face

    // --- Curved primitives ---
    public static Mesh uvSphere(Vec3 center, float radius, int rings, int segments);

    public static Mesh cylinder(Vec3 base, Vec3 axis, float radius, float height,
                                 int radialSegments, boolean capped);

    public static Mesh cone(Vec3 base, Vec3 axis, float radius, float height,
                             int radialSegments, boolean capped);

    public static Mesh torus(Vec3 center, Vec3 axis, float majorRadius, float minorRadius,
                              int majorSegments, int minorSegments);

    // --- Composition ---
    public static Mesh merge(Mesh... meshes); // concatenates vertex/index buffers with offset fixup
}
```

Each generator follows the same general computational-geometry approach (build a
parametrized grid or ring of vertices, triangulate consistently with CCW winding,
generate UVs in [0,1] over the shape's own parametrization) — implement each from
scratch; these are well-understood, self-contained algorithms and don't require
referencing prior art. `merge()` exists so a consumer can build a compound shape (e.g. a
cylinder + two cone caps, or several boxes) and treat it as one `Mesh` for deformation
and rendering purposes.

### 4.3 Deformation — a plain, shape-agnostic callback

FranklyLib has no opinion on *why* a mesh's vertices move — only on the shape of the
contract:

```java
@FunctionalInterface
public interface MeshDeformer {
    /** Returns deformed vertex positions, same length/order as the input mesh's vertices. */
    Vec3[] deform(Mesh baseMesh, float partialTick);

    MeshDeformer IDENTITY = (mesh, pt) -> {
        Vec3[] out = new Vec3[mesh.vertices().length];
        for (int i = 0; i < out.length; i++) out[i] = mesh.vertices()[i].position();
        return out;
    };
}
```

A consumer wanting physics-driven jiggle, skeletal-style bone weighting, a simple
sine-wave sway, or a static unmoving shape all implement this exact same interface —
FranklyLib doesn't need to know which. `MeshDeformer.IDENTITY` covers the "just render
the mesh as-is, no deformation" case out of the box.

---

## 5. Rendering (`com.frank1o3.franklylib.client.render`)

Two independent rendering entry points, since not every mesh needs to be entity-attached
— some mods will want to draw arbitrary geometry at an arbitrary world or GUI-space
transform with no entity involved at all.

### 5.1 `CustomGeometryRenderCommand` (generic, entity-independent)

A single reusable `SubmitNodeCollector.CustomGeometryRenderer` implementation that takes
already-deformed vertices + a `Mesh`'s indices/UVs + light/overlay/color and submits
triangles. Has no knowledge of entities, attachment points, or what the mesh represents
— it's the lowest-level building block everything else in this section is built from.

```java
public record CustomGeometryRenderCommand(
        Mesh mesh, Vec3[] deformedPositions,
        int light, int overlay, int color
) implements SubmitNodeCollector.CustomGeometryRenderer {
    @Override
    public void render(PoseStack.Pose pose, VertexConsumer vertexConsumer) {
        // walk mesh.indices() in triples, look up deformedPositions + mesh vertex UVs,
        // compute flat per-triangle normals, submit each vertex through pose's matrices
    }
}
```

Usable directly for free-floating geometry: push whatever `PoseStack` transform you want
(world position, GUI-space transform, wherever), submit a `CustomGeometryRenderCommand`,
pop. No entity required.

### 5.2 Entity attachment (optional layer on top)

For the common case of binding a mesh to a point on a living entity's model, rather than
managing the `PoseStack` transform by hand every time:

```java
public record AttachmentPoint(
        String targetPart,     // e.g. "head", "body", "left_arm" — matches a ModelPart lookup key
        Vec3 localOffset,
        Vec3 localRotationEuler,
        float localScale
) {}

public final class FranklyAttachmentRenderer {
    public static <S extends HumanoidRenderState, M extends HumanoidModel<S>> void render(
            PoseStack matrixStack,
            SubmitNodeCollector renderQueue,
            S state,
            M model,
            AttachmentPoint attachment,
            Mesh baseMesh,
            MeshDeformer deformer,
            RenderType renderType,
            int light, int overlay, int color,
            float partialTick
    ) {
        // 1. push pose
        // 2. resolve `targetPart` against the model's ModelPart tree (or a caller-supplied
        //    Function<M, ModelPart> lookup, for non-humanoid models) and apply its current
        //    pose transform, so the attachment follows limb swing / head look for free
        // 3. apply attachment's own local offset/rotation/scale on top
        // 4. deformer.deform(baseMesh, partialTick) -> Vec3[]
        // 5. submit via CustomGeometryRenderCommand
        // 6. pop pose
    }
}
```

This is intentionally a static helper rather than a `RenderLayer` itself, so any mod's
own `RenderLayer` can call into it without fighting layer-registration order — the
caller owns *when* this runs; FranklyLib only owns *how* the mesh gets from "deformed
vertex list" to "submitted triangles positioned relative to a body part."

### 5.3 GUI-space rendering (optional, for 3D elements inside a screen)

A small companion helper for rendering a `Mesh` inside a 2D screen (e.g. a rotating
preview object, a custom icon built from geometry instead of a texture) using the same
`CustomGeometryRenderCommand`, just with a `PoseStack` built from screen coordinates +
an orthographic-ish projection instead of an entity's world transform. Lower priority
than 5.1/5.2 — build if/when a concrete use case needs it, since the exact projection
setup depends on how "3D in a 2D screen" is being composed elsewhere in a given mod.

---

## 6. Suggested Build Order

1. Scaffold `franklylib` project (mod id, package, empty entrypoint, no dependencies
   beyond Fabric API/Loader).
2. Copy in the four base GUI files under their Frankly-prefixed names, add the
   `InventoryScreenAccessor` mixin, confirm they compile standalone.
3. `MeshVertex` / `Mesh` (common-side, no client deps) — unit-testable without a client
   (assert vertex/triangle counts, winding, index bounds for each generator).
4. `MeshBuilder`: implement `box`/`subdividedBox`/`plane` first (simplest), then
   `uvSphere`/`cylinder`/`cone`, then `torus` and `merge` last.
5. `MeshDeformer` interface + `MeshDeformer.IDENTITY`.
6. `CustomGeometryRenderCommand` — validate by rendering a static (identity-deformed)
   sphere or box floating at a fixed world position, with no entity involved at all.
7. `AttachmentPoint` + `FranklyAttachmentRenderer` — validate against a throwaway static
   mesh attached to, say, an entity's head, confirming it follows head rotation.
8. `FranklyCheckbox`, `FranklyTabBar` (simplest new widgets, good warm-up).
9. `FranklyScrollList`.
10. `FranklyDropdown` (built on `FranklyScrollList`).
11. `FranklyTextBox`.
12. `PaginatedContent` + optional `FranklyTooltipUtils` + optional Section 5.3
    GUI-space mesh rendering.
13. Publish to Modrinth once steps 1–11 are solid and demonstrated against at least one
    throwaway test screen/mesh, so the public API has been exercised end-to-end before
    it's locked in by external consumers.
