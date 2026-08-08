# UI animations

Animations are optional. A widget without `.animation(...)` renders exactly as it did before.

Resource packs define a preset at `assets/<namespace>/franklylib/ui_animations/<name>.json` and widgets opt in with `Identifier.fromNamespaceAndPath("<namespace>", "<name>")`.

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

Current widget support: `FranklyButton`. More widgets can consume `FranklyUiAnimations.frame(...)` while preserving no-animation as their default.
