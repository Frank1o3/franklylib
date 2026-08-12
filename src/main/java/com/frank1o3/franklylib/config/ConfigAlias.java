package com.frank1o3.franklylib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alternate JSON keys this entry should also be read from, checked in order
 * after {@link ConfigEntry#id()} fails to match. Lets a field be renamed
 * across mod versions without an old save file silently resetting that value
 * to its default. Only affects reads; new saves are always written under the
 * current {@link ConfigEntry#id()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT })
public @interface ConfigAlias {
    String[] value();
}
