package com.frank1o3.franklylib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Clamps a numeric {@link ConfigEntry} into {@code [min, max]} while loading
 * JSON. Ignored by non-numeric handlers. Also read by generic UI generation
 * (e.g. a Mod Menu screen) to pick a slider's bounds.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT })
public @interface Range {
    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;
}
