package com.frank1o3.franklylib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field, record component, or getter/setter pair as part of a
 * {@link FranklyConfigHolder}'s persisted schema.
 *
 * <p>
 * Applies to three access styles, auto-detected by {@link ConfigSchema}:
 * <ul>
 * <li>A public, non-final field — read/written directly.</li>
 * <li>A private field with a matching getter (e.g. {@code size()} or
 * {@code getSize()}) and setter ({@code setSize(...)}) — invoked via
 * reflection.</li>
 * <li>A record component — records are immutable, so changes go through
 * {@link FranklyConfigHolder#update}, which rebuilds the record via its
 * canonical constructor.</li>
 * </ul>
 *
 * <p>
 * {@code id()} is the JSON key used on disk; if left blank the member's own
 * name is used. Renaming a field later without breaking old save files is
 * done with {@link ConfigAlias}, not by keeping the old {@code id()} forever.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT })
public @interface ConfigEntry {
    /** JSON key on disk. Defaults to the annotated member's name. */
    String id() default "";

    /** Optional human-readable description, surfaced to tools like a Mod Menu screen. */
    String comment() default "";
}
