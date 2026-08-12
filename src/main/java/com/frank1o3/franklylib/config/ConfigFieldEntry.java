package com.frank1o3.franklylib.config;

import java.util.List;

/**
 * One resolved {@code @ConfigEntry} member: where its value lives
 * ({@link #accessor}), how to read/write it as JSON ({@link #handler}), and
 * its constraints. Built once per class by {@link ConfigSchema} and cached —
 * this is deliberately not re-derived on every save/load.
 */
public final class ConfigFieldEntry {

    private final String id;
    private final String comment;
    private final List<String> aliases;
    private final Class<?> type;
    private final double min;
    private final double max;

    /** Null for a leaf value entry; set instead of {@link #handler} for a nested group. */
    private final ConfigSchema nestedSchema;
    /** Null for a nested group entry. */
    private final ConfigValueHandler<Object> handler;

    private final ConfigAccessor accessor;

    ConfigFieldEntry(String id, String comment, List<String> aliases, Class<?> type, double min, double max,
            ConfigSchema nestedSchema, ConfigValueHandler<Object> handler, ConfigAccessor accessor) {
        this.id = id;
        this.comment = comment;
        this.aliases = aliases;
        this.type = type;
        this.min = min;
        this.max = max;
        this.nestedSchema = nestedSchema;
        this.handler = handler;
        this.accessor = accessor;
    }

    public String id() {
        return id;
    }

    public String comment() {
        return comment;
    }

    public List<String> aliases() {
        return aliases;
    }

    public Class<?> type() {
        return type;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public boolean isNestedGroup() {
        return nestedSchema != null;
    }

    ConfigSchema nestedSchema() {
        return nestedSchema;
    }

    ConfigValueHandler<Object> handler() {
        return handler;
    }

    ConfigAccessor accessor() {
        return accessor;
    }
}
