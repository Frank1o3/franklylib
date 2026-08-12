package com.frank1o3.franklylib.config;

/**
 * Signals that one JSON value cannot be read as its config entry's type.
 *
 * <p>The config engine catches this exception per entry, logs a warning, and
 * preserves that entry's previous value. Custom {@link ConfigValueHandler}
 * implementations should throw it from {@link ConfigValueHandler#fromJson}
 * when their input is invalid.</p>
 */
public class ConfigValueException extends RuntimeException {
    public ConfigValueException(String message) {
        super(message);
    }

    public ConfigValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
