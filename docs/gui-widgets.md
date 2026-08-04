# GUI Widgets

All widgets live under `com.frank1o3.franklylib.client.gui` and are client-only
(`@Environment(EnvType.CLIENT)`). Each widget follows the builder pattern —
`WidgetName.builder()....build()` — with `.bounds(x, y, width, height)` as the common
starting call.

All widgets share a flat visual language: solid semi-transparent fill, a lighter fill
on hover/focus, a darker fill when disabled, and a thin border where applicable. Build
your own widgets to match this palette if you want them to feel native to the kit.

---

## FranklyButton

A flat-style button. Wraps vanilla `Button` but replaces its rendering with the
FranklyLib look.

```java
FranklyButton button = FranklyButton.builder()
        .bounds(x, y, 86, 20)
        .message(Component.literal("Reset"))
        .onPress(btn -> doReset())
        .active(true)
        .build();
```

| Builder method | Description |
| --- | --- |
| `.bounds(x, y, width, height)` | Position and size |
| `.message(Component)` | Button label |
| `.onPress(OnPress)` | Click callback |
| `.active(boolean)` | Whether the button is enabled (default `true`) |

Hovering an active button requests a pointing-hand cursor; hovering a disabled one
requests a not-allowed cursor.

---

## FranklySlider

A generic floating-point slider. Supports mouse drag, mouse wheel, and keyboard
(arrow keys to nudge, Home/End to jump to the ends, Enter to commit).

```java
FranklySlider slider = FranklySlider.builder()
        .bounds(x, y, 200, 20)
        .range(0.1, 16.0)
        .step(0.1)
        .initialValue(1.0)
        .label(Component.literal("Scale"))
        .formatterString(v -> String.format("%.1fx", v))
        .onValueChanged(v -> preview(v))   // fires continuously while dragging
        .onValueCommitted(v -> commit(v))  // fires once on release/Enter/Home/End/scroll
        .build();
```

| Builder method | Description |
| --- | --- |
| `.bounds(x, y, width, height)` | Position and size |
| `.range(min, max)` | Inclusive value range; `max` must be `> min` |
| `.step(step)` | Snap increment; `0` or negative disables snapping |
| `.initialValue(value)` | Value shown on first render |
| `.label(Component)` | Optional prefix label, `null` shows only the formatted value |
| `.formatter(Function<Double, Component>)` | Custom value → label formatting |
| `.formatterString(Function<Double, String>)` | Convenience overload returning a plain string |
| `.onValueChanged(Consumer<Double>)` | Called continuously while the value is moving |
| `.onValueCommitted(Consumer<Double>)` | Called once when a change is finalized |

`getValue()` / `setValue(double)` and `getNormalized()` / `setNormalized(double)` are
available on the built instance for programmatic reads/updates.

---

## FranklyDropdown\<T>

A closed-state button that expands into an inline option list on click.

```java
FranklyDropdown<GameMode> dropdown = FranklyDropdown.<GameMode>builder()
        .bounds(x, y, 120, 20)
        .options(List.of(GameMode.SURVIVAL, GameMode.CREATIVE))
        .current(GameMode.SURVIVAL)
        .labelMapper(mode -> Component.translatable(mode.getKey()))
        .onSelect(mode -> applyMode(mode))
        .build();
```

| Builder method | Description |
| --- | --- |
| `.bounds(x, y, width, height)` | Position and size of the closed button |
| `.options(List<T>)` | Selectable values |
| `.current(T)` | Initially-selected value |
| `.labelMapper(Function<T, Component>)` | Value → display label |
| `.onSelect(Consumer<T>)` | Called when the user picks an option |

`getCurrent()` returns the currently selected value.

> **Note:** the current implementation renders its option list inline as part of its
> own `extractWidgetRenderState`, rather than as a screen-level overlay routed through
> the owning screen. This means it can be visually clipped or overdrawn by widgets
> below it and won't auto-close on an outside click unless the screen wires that up
> itself. Keep this in mind if you're placing a dropdown near the bottom of a panel.

---

## FranklyTextBox

Single-line text input with a blinking caret and horizontal scroll when content
overflows the field.

```java
FranklyTextBox textBox = FranklyTextBox.builder()
        .bounds(x, y, 120, 20)
        .initialValue("")
        .filter(s -> s.chars().allMatch(Character::isDigit))
        .maxLength(6)
        .onChanged(s -> validate(s))
        .onSubmit(s -> apply(s))
        .build();
```

| Builder method | Description |
| --- | --- |
| `.bounds(x, y, width, height)` | Position and size |
| `.initialValue(String)` | Starting text |
| `.filter(Predicate<String>)` | Rejects keystrokes that would produce a non-matching value |
| `.maxLength(int)` | Maximum character count; `-1` (default) disables the limit |
| `.onChanged(Consumer<String>)` | Called on every accepted keystroke |
| `.onSubmit(Consumer<String>)` | Called on Enter |

Accepts printable ASCII (`0x20`–`0x7E`); Backspace, Enter, and Left/Right arrow keys
are handled for editing and cursor movement.

---

## FranklyCheckbox

A small toggle square with a label to its right.

```java
FranklyCheckbox checkbox = FranklyCheckbox.builder()
        .bounds(x, y, 14, 14)
        .label(Component.literal("Enable feature"))
        .checked(false)
        .onToggle(value -> setEnabled(value))
        .build();
```

| Builder method | Description |
| --- | --- |
| `.bounds(x, y, width, height)` | Position and size of the checkbox square (label is drawn outside this box) |
| `.label(Component)` | Text drawn to the right of the box |
| `.checked(boolean)` | Initial state |
| `.onToggle(Consumer<Boolean>)` | Called on click or Space when focused |

`isChecked()` / `setChecked(boolean)` are available for programmatic reads/updates.

---

## FranklyTabBar\<T>

A row of equal-width `FranklyButton`-styled segments, with the current tab shown as
inactive/non-clickable.

```java
FranklyTabBar<Section> tabs = FranklyTabBar.<Section>builder()
        .bounds(x, y, 200, 20)
        .tabs(List.of(Section.GENERAL, Section.ADVANCED))
        .labelMapper(section -> Component.translatable(section.getKey()))
        .current(Section.GENERAL)
        .onSelect(section -> switchTo(section))
        .build();
```

| Builder method | Description |
| --- | --- |
| `.bounds(x, y, totalWidth, height)` | Total bar size; segments split `totalWidth` evenly |
| `.tabs(List<T>)` | Tab values, in display order |
| `.labelMapper(Function<T, Component>)` | Value → display label |
| `.current(T)` | Initially-active tab |
| `.onSelect(Consumer<T>)` | Called when a different tab is clicked |

---

## FranklyTooltipUtils

A static helper for drawing a themed tooltip box, for widgets that want a tooltip
visually consistent with the flat widget style instead of vanilla's
`Tooltip.create(...)` styling.

```java
FranklyTooltipUtils.drawTooltip(graphics, font, mouseX, mouseY,
        Component.literal("Explains what this does"));
```

Vanilla tooltips via `AbstractWidget#setTooltip` remain perfectly fine to use for most
cases — reach for this only when you specifically need the flat theme.

---

## FranklyGuiUtils

Shared drawing helpers used internally by several widgets, and useful directly for
custom screens:

- `drawFittedText(Justify, graphics, font, text, left, top, right, bottom, color)` —
  draws text left-justified or centered in a box, auto-shrinking to a horizontal
  marquee scroll if it doesn't fit.
- `drawScaledEntityPreview(graphics, x1, y1, x2, y2, size, mouseX, mouseY, entity)` —
  renders a living entity inside a screen-space box, facing the cursor, **at its true
  current scale** (unlike vanilla's inventory preview, which normalizes scale back to
  1.0).

`drawScaledEntityPreview` depends on FranklyLib's own
`InventoryScreenAccessor` mixin, which is bundled and registered automatically — no
extra setup needed in your mod.

---

## Screen chrome — BaseScaleScreen

A base `Screen` subclass providing the shared panel look: a dimmed background overlay,
a bordered centered panel, and a title drawn at its top.

```java
public class MyScreen extends BaseScaleScreen {
    public MyScreen(Screen parent) {
        super(Component.literal("My Screen"), parent, 220, 160);
    }

    @Override
    protected void renderPanelContent(GuiGraphicsExtractor graphics, int panelX, int panelY,
            int mouseX, int mouseY, float delta) {
        // draw extra content inside the panel, before widgets
    }
}
```

`panelX()` / `panelY()` give the panel's top-left corner (valid after `init()`).
Override `renderPanelContent(...)` for content drawn between the panel chrome and its
child widgets. `onClose()` returns to the `parent` screen passed to the constructor.

> Despite the "Frankly" naming of every other widget in this file, this class is
> currently still named `BaseScaleScreen` rather than `BaseFranklyScreen`. Worth a
> rename before you publish, so the public API doesn't read like a leftover from a
> different mod.

---

## PaginatedContent\<T>

A plain utility (not a widget) for slicing a full list down to one page and reporting
prev/next availability, so screens don't hand-roll `startIndex`/`endIndex` bookkeeping.

```java
PaginatedContent<Item> page = PaginatedContent.of(allItems, currentPage, pageSize);

page.items();        // visible slice for this page
page.page();          // clamped current page index
page.totalPages();
page.hasPrevious();
page.hasNext();
```

`page` is clamped into `[0, totalPages - 1]`, so passing an out-of-range page index is
safe.
