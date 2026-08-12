# Config Engine

The config engine lives in `com.frank1o3.franklylib.config` and is common-side: it
can be used for a dedicated-server config, a client config, or one holder on each
side. It stores one annotated Java object in one JSON file and deliberately does
not provide networking or decide which side is authoritative. Your mod owns that.

The main entry point is `FranklyConfigHolder<T>`. It creates the file with defaults
on first run, preserves valid values when individual JSON entries are missing or
invalid, can save changes automatically, and can pick up external edits safely.

---

## Basic setup

Create a plain mutable class and mark every persisted field with `@ConfigEntry`.
Public, non-final fields are the simplest option.

```java
import com.frank1o3.franklylib.config.ConfigEntry;
import com.frank1o3.franklylib.config.FranklyConfigHolder;
import com.frank1o3.franklylib.config.Range;
import net.fabricmc.loader.api.FabricLoader;

public final class MyConfig {
    @ConfigEntry(comment = "Whether the feature is enabled")
    public boolean enabled = true;

    @ConfigEntry
    @Range(min = 0.25, max = 16.0)
    public double scale = 1.0;
}

FranklyConfigHolder<MyConfig> config = FranklyConfigHolder
        .builder(MyConfig.class, MyConfig::new)
        .path(FabricLoader.getInstance().getConfigDir().resolve("mymod.json"))
        .autosaveTicks(20 * 60 * 5)
        .staleCheckTicks(20 * 10)
        .build();
```

`build()` reads an existing file immediately. If the file is absent, it uses the
value produced by `MyConfig::new` and writes it to disk. Keep the holder for the
whole lifetime of the relevant side; do not rebuild it every tick or every screen
open.

The above produces JSON similar to:

```json
{
  "enabled": true,
  "scale": 1.0
}
```

`comment` is schema metadata for tools such as a future config screen. JSON itself
does not support comments, so it is not written into the file.

---

## Reading, changing, and lifecycle

Read the current value with `get()`:

```java
if (config.get().enabled) {
    applyScale(config.get().scale);
}
```

Make every change through `update(...)`. It marks the holder dirty so autosave can
write it, and works for both mutable classes and records:

```java
config.update(current -> {
    current.enabled = false;
    current.scale = 1.5;
    return current;
});
```

Call `tick()` once per game tick on the side that owns the holder. It drives both
autosave and optional external-file checks. On that side's shutdown, call
`flushAndClose()` to write pending changes and stop its background I/O executor.

```java
// Register these with your mod's own client or server lifecycle/tick events.
config.tick();

// At shutdown:
config.flushAndClose();
```

Use `saveIfDirty()` for an asynchronous save, `save()` when an immediate synchronous
write is needed, and `forceReload()` to explicitly reload the file. `onSave(...)`
and `onReload(...)` register callbacks that receive the saved or reloaded value.

When the file changes outside the game, `tick()` reloads it after the configured
`staleCheckTicks` interval. Unsaved in-memory changes win over an external edit, so
the engine never silently overwrites a change your mod has not saved yet.

---

## Entries and supported values

Every persisted member needs `@ConfigEntry`. If `id` is not specified, the Java
member name becomes the JSON key.

```java
@ConfigEntry(id = "max_scale", comment = "Maximum permitted scale")
@ConfigAlias({ "maximumScale", "scaleLimit" })
@Range(min = 0.25, max = 16.0)
public double maxScale = 4.0;
```

| Annotation | Purpose |
| --- | --- |
| `@ConfigEntry` | Marks a member for persistence. `id` changes its JSON key; `comment` supplies tool-facing documentation. |
| `@ConfigAlias` | Lists old JSON keys to read after the current `id`, allowing a field rename without losing existing values. New saves use only the current key. |
| `@Range` | Sets inclusive numeric bounds applied while JSON is read. It is ignored for non-numeric values. |

Built-in value handlers support `double`, `float`, `int`, `long`, `boolean`,
`String`, their boxed forms, enums, and Minecraft `Identifier`. Enums are stored by
their constant name; identifiers are stored as strings such as `"minecraft:stone"`.

An unsupported field type is treated as a nested config group when its own class has
`@ConfigEntry` members:

```java
public final class MyConfig {
    @ConfigEntry
    public DisplayConfig display = new DisplayConfig();
}

public final class DisplayConfig {
    @ConfigEntry public boolean showOverlay = true;
    @ConfigEntry @Range(min = 0.5, max = 4.0) public float opacity = 1.0f;
}
```

Nested groups are serialized as JSON objects. Circular nesting is rejected when the
schema is built.

For another leaf type, register a `ConfigValueHandler<T>` before building a holder
that uses it:

```java
ConfigValueHandlers.register(MyValue.class, new ConfigValueHandler<>() {
    @Override
    public JsonElement toJson(MyValue value) { /* return JSON */ }

    @Override
    public MyValue fromJson(JsonElement json) {
        if (!isValid(json)) {
            throw new ConfigValueException("Expected a valid MyValue");
        }
        return parse(json);
    }

    @Override
    public MyValue clamp(MyValue value, ConfigFieldEntry entry) { return value; }
});
```

`fromJson` should throw `ConfigValueException` for invalid data. The engine treats
that entry as invalid, logs a warning, and retains its previous/default value rather
than discarding the whole config file.

---

## Private fields and records

The engine can reflectively use private fields. If a matching getter and setter are
present, it uses them; otherwise it reads and writes the field directly. Supported
getter names are `getName()`, `isName()`, and `name()`; the setter must be
`setName(value)`.

Records are also supported. Annotate their components and return a replacement value
from `update(...)`:

```java
public record ClientConfig(
        @ConfigEntry boolean showOverlay,
        @ConfigEntry @Range(min = 0.5, max = 4.0) float opacity) {}

config.update(old -> new ClientConfig(false, old.opacity()));
```

For a record, all persisted constructor components should be annotated. The engine
uses its canonical constructor when it reloads the JSON file.

---

## Validation and failure behavior

Validation happens per entry. A malformed `scale` value does not prevent a valid
`enabled` value from loading: the invalid entry remains at its previous value and a
warning is logged. A completely unparsable file leaves the in-memory config alone.

`ConfigSchemaException` indicates a setup mistake that the mod author must fix, such
as an empty schema, cyclic nested groups, or an unhandled field type. It is raised
while the holder is built rather than hidden at runtime.
