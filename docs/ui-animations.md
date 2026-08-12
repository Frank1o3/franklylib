# UI animations

Animations are optional. A widget without `.animation(...)` renders exactly as it did before.

Resource packs define a preset at `assets/<namespace>/ui_animations/<name>.json` and widgets opt in with `Identifier.fromNamespaceAndPath("<namespace>", "<name>")`.

```json
{
  "duration_ms": 120,
  "easing": "out_quad",
  "states": {
    "idle": { "scale": 1.0, "alpha": 1.0 },
    "hover": { "translate_y": -1.0, "scale": 1.035 },
    "disabled": { "alpha": 0.65 }
  }
}
```

Supported fields per state are `translate_x`, `translate_y`, `scale`, and `alpha`; omitted fields use identity values. Supported easing names are `linear`, `in_quad`, `out_quad`, and `in_out_quad`.

Current widget support: `FranklyButton`, `FranklySlider`, and `FranklyNumberInput`.

## Asset location

`FranklyUiAnimations.setResourceDirectory("my_mod/ui_animations")` changes the path
scanned below every `assets/<namespace>` root on the next resource reload. Call it during
your client initialisation before resource packs load. This lets a mod keep animation
assets under its own asset subtree while resource packs can still override the same
namespace/path.
