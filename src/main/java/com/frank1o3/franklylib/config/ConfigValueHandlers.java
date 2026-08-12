package com.frank1o3.franklylib.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central lookup from a Java type to the {@link ConfigValueHandler} that
 * knows how to read/write/clamp it. Ships handlers for primitives, String,
 * enums (resolved dynamically, not pre-registered), and {@link Identifier}.
 *
 * <p>
 * Any other mod can call {@link #register} for its own types before building
 * a {@link FranklyConfigHolder} that uses them — this is what keeps the
 * engine from being hardcoded to whatever types FranklyLib's own mods
 * happen to use.
 */
public final class ConfigValueHandlers {

    private static final Map<Class<?>, ConfigValueHandler<Object>> HANDLERS = new ConcurrentHashMap<>();

    private ConfigValueHandlers() {
    }

    /** Registers (or overrides) the handler used for {@code type}. */
    public static <T> void register(Class<T> type, ConfigValueHandler<T> handler) {
        // Field.get and method invocation always box primitive values. Class.cast,
        // however, does not unbox: boolean.class.cast(Boolean.TRUE) throws a
        // ClassCastException. Normalize primitive tokens to their boxed class at
        // this boundary so primitive and wrapper config fields share a handler.
        Class<?> valueType = boxedType(type);
        HANDLERS.put(type, new ConfigValueHandler<>() {
            @Override
            public JsonElement toJson(Object value) {
                return handler.toJson(castValue(valueType, value));
            }

            @Override
            public Object fromJson(JsonElement json) {
                return handler.fromJson(json);
            }

            @Override
            public Object clamp(Object value, ConfigFieldEntry entry) {
                return handler.clamp(castValue(valueType, value), entry);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T castValue(Class<?> type, Object value) {
        return (T) type.cast(value);
    }

    private static Class<?> boxedType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == void.class) return Void.class;
        throw new IllegalArgumentException("Unknown primitive type: " + type);
    }

    /**
     * Returns the handler for {@code type}, or {@code null} if none is
     * registered and {@code type} isn't an enum. A {@code null} result
     * signals the caller (schema building) to try treating the field as a
     * nested group of its own {@code @ConfigEntry} members instead.
     */
    public static ConfigValueHandler<Object> forType(Class<?> type) {
        ConfigValueHandler<Object> direct = HANDLERS.get(type);
        if (direct != null) {
            return direct;
        }
        if (type.isEnum()) {
            return enumHandler(type);
        }
        return null;
    }

    // -------------------------------------------------------------------
    // Built-ins
    // -------------------------------------------------------------------

    private static final ConfigValueHandler<Double> DOUBLE = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(Double value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Double fromJson(JsonElement json) {
            requireNumber(json);
            return json.getAsDouble();
        }

        @Override
        public Double clamp(Double value, ConfigFieldEntry entry) {
            return Math.max(entry.min(), Math.min(entry.max(), value));
        }
    };

    private static final ConfigValueHandler<Float> FLOAT = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(Float value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Float fromJson(JsonElement json) {
            requireNumber(json);
            return json.getAsFloat();
        }

        @Override
        public Float clamp(Float value, ConfigFieldEntry entry) {
            return (float) Math.max(entry.min(), Math.min(entry.max(), value));
        }
    };

    private static final ConfigValueHandler<Integer> INT = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(Integer value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Integer fromJson(JsonElement json) {
            requireNumber(json);
            return json.getAsInt();
        }

        @Override
        public Integer clamp(Integer value, ConfigFieldEntry entry) {
            return (int) Math.max(entry.min(), Math.min(entry.max(), value));
        }
    };

    private static final ConfigValueHandler<Long> LONG = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(Long value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Long fromJson(JsonElement json) {
            requireNumber(json);
            return json.getAsLong();
        }

        @Override
        public Long clamp(Long value, ConfigFieldEntry entry) {
            return (long) Math.max(entry.min(), Math.min(entry.max(), value));
        }
    };

    private static final ConfigValueHandler<Boolean> BOOL = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Boolean fromJson(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isBoolean()) {
                throw new ConfigValueException("Expected a boolean, got " + json);
            }
            return json.getAsBoolean();
        }

        @Override
        public Boolean clamp(Boolean value, ConfigFieldEntry entry) {
            return value;
        }
    };

    private static final ConfigValueHandler<String> STRING = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(String value) {
            return new JsonPrimitive(value == null ? "" : value);
        }

        @Override
        public String fromJson(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
                throw new ConfigValueException("Expected a string, got " + json);
            }
            return json.getAsString();
        }

        @Override
        public String clamp(String value, ConfigFieldEntry entry) {
            return value;
        }
    };

    private static final ConfigValueHandler<Identifier> IDENTIFIER = new ConfigValueHandler<>() {
        @Override
        public JsonElement toJson(Identifier value) {
            return new JsonPrimitive(value == null ? "" : value.toString());
        }

        @Override
        public Identifier fromJson(JsonElement json) {
            if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
                throw new ConfigValueException("Expected an identifier string, got " + json);
            }
            Identifier parsed = Identifier.tryParse(json.getAsString());
            if (parsed == null) {
                throw new ConfigValueException("Not a valid identifier: " + json.getAsString());
            }
            return parsed;
        }

        @Override
        public Identifier clamp(Identifier value, ConfigFieldEntry entry) {
            return value;
        }
    };

    private static ConfigValueHandler<Object> enumHandler(Class<?> enumType) {
        return new ConfigValueHandler<>() {
            @Override
            public JsonElement toJson(Object value) {
                return new JsonPrimitive(((Enum<?>) value).name());
            }

            @Override
            public Object fromJson(JsonElement json) {
                if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isString()) {
                    throw new ConfigValueException("Expected an enum name string, got " + json);
                }
                for (Object constant : enumType.getEnumConstants()) {
                    Enum<?> enumConstant = (Enum<?>) constant;
                    if (enumConstant.name().equals(json.getAsString())) {
                        return enumConstant;
                    }
                }
                throw new ConfigValueException(
                        "Unknown constant '" + json.getAsString() + "' for enum " + enumType.getSimpleName());
            }

            @Override
            public Object clamp(Object value, ConfigFieldEntry entry) {
                return value;
            }
        };
    }

    private static void requireNumber(JsonElement json) {
        if (!json.isJsonPrimitive() || !json.getAsJsonPrimitive().isNumber()) {
            throw new ConfigValueException("Expected a number, got " + json);
        }
    }

    static {
        register(Double.class, DOUBLE);
        register(double.class, DOUBLE);
        register(Float.class, FLOAT);
        register(float.class, FLOAT);
        register(Integer.class, INT);
        register(int.class, INT);
        register(Long.class, LONG);
        register(long.class, LONG);
        register(Boolean.class, BOOL);
        register(boolean.class, BOOL);
        register(String.class, STRING);
        register(Identifier.class, IDENTIFIER);
    }
}
