package com.frank1o3.franklylib.config;

import com.google.gson.JsonElement;

/**
 * Converts one config value type to/from JSON and applies its constraints.
 * FranklyLib registers handlers for common types out of the box (see
 * {@link ConfigValueHandlers}); a mod using a type this doesn't know about
 * registers its own with {@link ConfigValueHandlers#register} — this is the
 * escape hatch that makes the config engine usable for arbitrary data, not
 * just what FranklyLib itself anticipated.
 *
 * @param <T> the boxed value type this handler reads/writes (e.g.
 *            {@code Double} for both {@code double} and {@code Double}
 *            fields — reflection already boxes primitives).
 */
public interface ConfigValueHandler<T> {

    JsonElement toJson(T value);

    /**
     * Parses a JSON value into {@code T}.
     *
     * @throws ConfigValueException if {@code json} isn't a valid {@code T} —
     *                               the caller catches this per-entry and
     *                               falls back to the previous value.
     */
    T fromJson(JsonElement json);

    /**
     * Applies this entry's constraints (e.g. {@link Range}) to {@code value}.
     * Handlers that don't have a relevant constraint just return
     * {@code value} unchanged.
     */
    T clamp(T value, ConfigFieldEntry entry);
}
