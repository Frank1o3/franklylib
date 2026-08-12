package com.frank1o3.franklylib.config;

/**
 * Thrown when reflection can't reach a field/getter/setter/record accessor
 * it already resolved at schema-build time (should be rare — usually means a
 * security manager or module boundary is blocking {@code setAccessible}).
 */
class ConfigAccessException extends RuntimeException {
    ConfigAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Thrown at schema-build time (once per class, cached) when a type can't be
 * turned into a schema at all: no {@link ConfigEntry} members found, a
 * cyclic nested group, or a field type with no registered
 * {@link ConfigValueHandler} and no nested {@code @ConfigEntry} members of
 * its own. Unlike {@link ConfigValueException}, this is a setup-time mistake
 * for the mod author to fix, not a runtime data problem — it is not caught
 * and swallowed.
 */
public class ConfigSchemaException extends RuntimeException {
    public ConfigSchemaException(String message) {
        super(message);
    }

    public ConfigSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
