# FranklyLib

FranklyLib is a small, dependency-free [Fabric](https://fabricmc.net/) library mod that
gives other mods two things:

1. **A cohesive custom GUI widget kit** — buttons, sliders, dropdowns, text boxes,
   checkboxes, and tab bars that all share one flat, semi-transparent visual language,
   so a screen built from them doesn't end up mixing vanilla's beveled widgets with
   ad-hoc custom ones.
2. **A general-purpose triangle mesh API** — build arbitrary meshes (procedural
   primitives or fully custom vertex/index data), optionally deform their vertices
   per-frame via a plug-in callback, and render them either as free-floating 3D
   geometry or attached to a point on a living entity's model.

FranklyLib has **no gameplay of its own** and ships **no networking code**. It's a
toolkit other mods depend on, not a mod players install standalone.

## Why

Fabric mods that need custom UI or custom 3D geometry tend to reinvent the same
handful of widgets and the same mesh-building boilerplate every time. FranklyLib pulls
that into one reusable library so you can focus on your mod's actual logic instead of
re-deriving "how do I draw a slider" or "how do I triangulate a sphere" from scratch.

## Requirements

| | |
| --- | --- |
| Minecraft | 26.2 |
| Loader | Fabric Loader ≥ 0.19.3 |
| Java | 25+ |
| Fabric API | **Not required.** FranklyLib only uses Fabric Loader (`net.fabricmc.api`), not Fabric API. |

## Installation

> **Not yet published.** The snippet below will work once a release has actually been
> pushed through the publish workflow — there's nothing at these coordinates until
> then.

FranklyLib publishes to its own self-hosted Maven repo via GitHub Pages. Add it as a
dependency in your loom project:

```gradle
repositories {
    maven { url = 'https://frank1o3.github.io/franklylib/maven/' }
}

dependencies {
    modImplementation "com.frank1o3:franklylib:${franklylib_version}"
}
```

Replace `${franklylib_version}` with an actual released version (matches
`mod_version` in `gradle.properties` at release time) — check the repo's
[Releases](https://github.com/frank1o3/franklylib/releases) page for available
versions.

FranklyLib is also published to [Modrinth](https://modrinth.com/), which hosts its own
Maven repo per project if you'd rather depend on that instead:

```gradle
repositories {
    maven { url = 'https://api.modrinth.com/maven' }
}

dependencies {
    modImplementation "maven.modrinth:franklylib:${franklylib_version}"
}
```

## Features

### GUI toolkit

All widgets live under `com.frank1o3.franklylib.client.gui` and follow the same
builder pattern:

```java
FranklyButton button = FranklyButton.builder()
        .bounds(x, y, 86, 20)
        .message(Component.literal("Reset"))
        .onPress(btn -> doReset())
        .build();

FranklySlider slider = FranklySlider.builder()
        .bounds(x, y, 200, 20)
        .range(0.1, 16.0)
        .step(0.1)
        .initialValue(1.0)
        .label(Component.literal("Scale"))
        .formatterString(v -> String.format("%.1fx", v))
        .onValueChanged(v -> preview(v))
        .onValueCommitted(v -> commit(v))
        .build();
```

See [`docs/gui-widgets.md`](docs/gui-widgets.md) for the full widget reference.

### Mesh construction & rendering

All mesh types live in `com.frank1o3.franklylib` (common-side, no client
dependencies), so they can be referenced from code that isn't client-only. Rendering
helpers live in `com.frank1o3.franklylib.client.render`.

```java
Mesh sphere = MeshBuilder.uvSphere(Vec3.ZERO, 1.0f, 12, 12).withComputedNormals();

FranklyAttachmentRenderer.render(
        poseStack, renderQueue, renderState, model,
        new AttachmentPoint("head", new Vec3(0, 0.2f, 0), Vec3.ZERO, 1.0f),
        sphere, MeshDeformer.IDENTITY, renderType,
        light, overlay, color, partialTick);
```

See [`docs/mesh-api.md`](docs/mesh-api.md) for the full mesh/rendering reference.

## What this library is *not*

- No physics simulation — deformation is a plain `(Mesh, float) -> Vec3[]` callback
  the consumer supplies.
- No per-entity or per-player config storage/caching.
- No networking or sync — consumers own their own sync entirely.
- No assumptions about what a mesh represents or where it's rendered.

## Documentation

- [GUI widget reference](docs/gui-widgets.md)
- [Mesh & rendering API reference](docs/mesh-api.md)

## License

FranklyLib is released under [CC0 1.0](LICENSE) — public domain. Use it, fork it,
strip it for parts, no attribution required.