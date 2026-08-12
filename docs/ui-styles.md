# UI styles

FranklyLib UI styles are small, resource-pack-driven JSON skins. They control colors,
content padding, and a square, rounded, or absent border; layout and widget behavior
remain in code.

Put a style at `assets/<namespace>/ui_styles/<name>.json`, then opt a widget in with
`.style(Identifier.fromNamespaceAndPath("<namespace>", "<name>"))`. The file path is
the style ID, so `assets/example/ui_styles/menu/primary.json` is `example:menu/primary`.

```json
{
  "padding": 4,
  "border": { "type": "rounded", "radius": 4, "width": 1 },
  "colors": {
    "background": "#CC1A1A2E",
    "hover_background": "#DD272743",
    "disabled_background": "#99202030",
    "border": "#FF3A3A5E",
    "text": "#FFFFFFFF",
    "disabled_text": "#FF888899",
    "accent": "#FF7A9CFF"
  }
}
```

All colors accept `#RRGGBB`, `#AARRGGBB`, or an integer ARGB value. Omitting a property
uses the library default. `border.type` is `square`, `rounded`, or `none`; `radius` and
`width` are pixels.

`FranklyButton`, `FranklySlider`, `FranklyCheckbox`, `FranklyTextBox`,
`FranklyNumberInput`, `FranklyDropdown`, and `FranklyTabBar` expose `.style(...)`. A `BaseFranklyScreen` subclass can call
`setUiStyle(...)` from its constructor. `FranklyScrollPanel.create(...).style(...)`,
`FranklyEntityPreviewWidget.builder().style(...)`, and the five-argument
`FranklyTooltipUtils.drawTooltip(..., style)` cover the remaining built-in UI pieces.
FranklyLib ships `franklylib:default` and
`franklylib:rounded` examples/default styles.

To place style files elsewhere below assets, call
`FranklyUiStyles.setResourceDirectory("my_mod/ui_styles")` in client initialisation
before resource packs load. Resource packs can use the exact same namespace and path
to replace a mod's style.
