package com.frank1o3.franklylib.config;

import com.google.gson.JsonObject;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The reflected, cached schema for one config class: every
 * {@code @ConfigEntry} member it declares, in declaration order, plus enough
 * information to serialize/deserialize/reconstruct instances of it.
 *
 * <p>
 * Built once per {@code Class<T>} (scanning is not free — {@code setAccessible}
 * and annotation lookups run once, then every {@link FranklyConfigHolder} for
 * that type reuses the same schema instance).
 */
public final class ConfigSchema {

    private static final Map<Class<?>, ConfigSchema> CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<Set<Class<?>>> BUILDING = ThreadLocal.withInitial(HashSet::new);

    private final Class<?> type;
    private final boolean isRecord;
    private final List<ConfigFieldEntry> entries;
    private final Constructor<?> recordConstructor; // null unless isRecord

    private ConfigSchema(Class<?> type, boolean isRecord, List<ConfigFieldEntry> entries,
            Constructor<?> recordConstructor) {
        this.type = type;
        this.isRecord = isRecord;
        this.entries = entries;
        this.recordConstructor = recordConstructor;
    }

    public Class<?> type() {
        return type;
    }

    public boolean isRecord() {
        return isRecord;
    }

    public List<ConfigFieldEntry> entries() {
        return entries;
    }

    /**
     * Returns the cached schema for {@code type}, building and caching it on
     * first use.
     *
     * @throws ConfigSchemaException if {@code type} has no {@code @ConfigEntry}
     *                                members, a cyclic nested group, or a
     *                                member type with neither a registered
     *                                {@link ConfigValueHandler} nor its own
     *                                {@code @ConfigEntry} members.
     */
    public static ConfigSchema of(Class<?> type) {
        ConfigSchema cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        Set<Class<?>> building = BUILDING.get();
        if (!building.add(type)) {
            throw new ConfigSchemaException("Cyclic nested config schema detected involving " + type.getName());
        }
        try {
            ConfigSchema built = build(type);
            CACHE.put(type, built);
            return built;
        } finally {
            building.remove(type);
        }
    }

    // -------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------

    private static ConfigSchema build(Class<?> type) {
        List<ConfigFieldEntry> entries = new ArrayList<>();

        if (type.isRecord()) {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
            }
            Constructor<?> canonical;
            try {
                canonical = type.getDeclaredConstructor(paramTypes);
                canonical.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new ConfigSchemaException("No canonical constructor found for record " + type.getName(), e);
            }

            for (RecordComponent component : components) {
                ConfigEntry annotation = component.getAnnotation(ConfigEntry.class);
                if (annotation == null) {
                    throw new ConfigSchemaException("Record component '" + component.getName() + "' in "
                            + type.getName() + " is missing @ConfigEntry. Every record component must be annotated.");
                }
                Method accessorMethod = component.getAccessor();
                entries.add(buildEntry(annotation, component, component.getName(), component.getType(),
                        new RecordComponentAccessor(accessorMethod)));
            }

            if (entries.isEmpty()) {
                throw new ConfigSchemaException(
                        "Record " + type.getName() + " has no components");
            }
            return new ConfigSchema(type, true, List.copyOf(entries), canonical);
        }

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            ConfigEntry annotation = field.getAnnotation(ConfigEntry.class);
            if (annotation == null) {
                continue;
            }

            ConfigAccessor accessor;
            if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                accessor = new FieldAccessor(field);
            } else {
                Method getter = findGetter(type, field.getName());
                Method setter = findSetter(type, field.getName(), field.getType());
                accessor = (getter != null && setter != null)
                        ? new PropertyAccessor(getter, setter)
                        : new FieldAccessor(field); // fall back to raw reflection on the private field
            }

            entries.add(buildEntry(annotation, field, field.getName(), field.getType(), accessor));
        }

        if (entries.isEmpty()) {
            throw new ConfigSchemaException("No @ConfigEntry members found on " + type.getName()
                    + " (checked fields; for records, annotate the record components instead)");
        }
        return new ConfigSchema(type, false, List.copyOf(entries), null);
    }

    private static ConfigFieldEntry buildEntry(ConfigEntry annotation, AnnotatedElement source, String memberName,
            Class<?> declaredType, ConfigAccessor accessor) {
        String id = annotation.id().isBlank() ? memberName : annotation.id();

        Range range = source.getAnnotation(Range.class);
        double min = range != null ? range.min() : Double.NEGATIVE_INFINITY;
        double max = range != null ? range.max() : Double.POSITIVE_INFINITY;

        ConfigAlias aliasAnnotation = source.getAnnotation(ConfigAlias.class);
        List<String> aliases = aliasAnnotation != null ? List.of(aliasAnnotation.value()) : List.of();

        ConfigValueHandler<Object> handler = ConfigValueHandlers.forType(declaredType);
        ConfigSchema nested = null;
        if (handler == null) {
            try {
                nested = ConfigSchema.of(declaredType);
            } catch (ConfigSchemaException nestedFailure) {
                throw new ConfigSchemaException("No ConfigValueHandler and no nested @ConfigEntry schema for '"
                        + memberName + "' (type " + declaredType.getName() + "). Register a handler via "
                        + "ConfigValueHandlers.register(...), or annotate that type's own fields.", nestedFailure);
            }
        }

        return new ConfigFieldEntry(id, annotation.comment(), aliases, declaredType, min, max, nested, handler,
                accessor);
    }

    private static Method findGetter(Class<?> type, String fieldName) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (String prefix : new String[] { "get", "is" }) {
            try {
                Method m = type.getDeclaredMethod(prefix + capitalized);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // try the next convention
            }
        }
        // bare-name convention, e.g. size()/petite() as already used elsewhere in this codebase
        try {
            Method m = type.getDeclaredMethod(fieldName);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findSetter(Class<?> type, String fieldName, Class<?> fieldType) {
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            Method m = type.getDeclaredMethod("set" + capitalized, fieldType);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    // -------------------------------------------------------------------
    // Serialization helpers used by FranklyConfigHolder
    // -------------------------------------------------------------------

    JsonObject serialize(Object instance) {
        JsonObject root = new JsonObject();
        for (ConfigFieldEntry entry : entries) {
            Object value = entry.accessor().get(instance);
            if (entry.isNestedGroup()) {
                root.add(entry.id(), serializeNested(entry, value));
            } else {
                root.add(entry.id(), entry.handler().toJson(value));
            }
        }
        return root;
    }

    private static JsonObject serializeNested(ConfigFieldEntry entry, Object nestedInstance) {
        return entry.nestedSchema().serialize(nestedInstance);
    }

    /**
     * Reads {@code root} against this schema, falling back per-entry to the
     * matching value on {@code fallback} whenever a key is missing or
     * invalid. Mutable (non-record) types are updated in place on
     * {@code fallback} and the same reference is returned. Records are
     * rebuilt via {@link #reconstructRecord} since they can't be mutated.
     */
    Object mergeFromJson(JsonObject root, Object fallback, ConfigWarnSink warnings) {
        if (isRecord) {
            Object[] args = new Object[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                args[i] = resolveEntryValue(entries.get(i), root, fallback, warnings);
            }
            return reconstructRecord(args);
        }

        for (ConfigFieldEntry entry : entries) {
            Object resolved = resolveEntryValue(entry, root, fallback, warnings);
            entry.accessor().set(fallback, resolved);
        }
        return fallback;
    }

    private static Object resolveEntryValue(ConfigFieldEntry entry, JsonObject root, Object fallbackOwner,
            ConfigWarnSink warnings) {
        com.google.gson.JsonElement json = findJsonForEntry(entry, root);
        Object previous = entry.accessor().get(fallbackOwner);

        if (json == null || json.isJsonNull()) {
            return previous;
        }

        try {
            if (entry.isNestedGroup()) {
                if (!json.isJsonObject()) {
                    throw new ConfigValueException("Expected an object for nested group '" + entry.id() + "'");
                }
                return entry.nestedSchema().mergeFromJson(json.getAsJsonObject(), previous, warnings);
            }
            Object value = entry.handler().fromJson(json);
            return entry.handler().clamp(value, entry);
        } catch (ConfigValueException e) {
            warnings.warn("Field '" + entry.id() + "' is invalid (" + e.getMessage() + "); keeping previous value");
            return previous;
        }
    }

    private static com.google.gson.JsonElement findJsonForEntry(ConfigFieldEntry entry, JsonObject root) {
        if (root.has(entry.id())) {
            return root.get(entry.id());
        }
        for (String alias : entry.aliases()) {
            if (root.has(alias)) {
                return root.get(alias);
            }
        }
        return null;
    }

    Object reconstructRecord(Object[] args) {
        try {
            return recordConstructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ConfigAccessException("Cannot construct record " + type.getName(), e);
        }
    }

    /** Small seam so callers can route warnings to their own logger instead of a static one. */
    interface ConfigWarnSink {
        void warn(String message);
    }
}
