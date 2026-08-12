package com.frank1o3.franklylib.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reads (and, where possible, writes) one config member regardless of
 * whether it's backed by a public field, a private field with a
 * getter/setter pair, or a record component. {@link ConfigSchema} picks the
 * right implementation per member at schema-build time.
 */
interface ConfigAccessor {
    Object get(Object owner);

    /**
     * Writes {@code value} onto {@code owner}. Record components are
     * immutable and don't support this — see
     * {@link RecordComponentAccessor#set}.
     */
    void set(Object owner, Object value);

    Class<?> type();
}

/** Direct reflective field access — used for public mutable fields, and as the fallback for private ones. */
final class FieldAccessor implements ConfigAccessor {
    private final Field field;

    FieldAccessor(Field field) {
        field.setAccessible(true);
        this.field = field;
    }

    @Override
    public Object get(Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new ConfigAccessException("Cannot read field " + field, e);
        }
    }

    @Override
    public void set(Object owner, Object value) {
        try {
            field.set(owner, value);
        } catch (IllegalAccessException e) {
            throw new ConfigAccessException("Cannot write field " + field, e);
        }
    }

    @Override
    public Class<?> type() {
        return field.getType();
    }
}

/** Getter/setter method pair access — used for private fields exposed via accessor methods. */
final class PropertyAccessor implements ConfigAccessor {
    private final Method getter;
    private final Method setter;

    PropertyAccessor(Method getter, Method setter) {
        getter.setAccessible(true);
        setter.setAccessible(true);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object get(Object owner) {
        try {
            return getter.invoke(owner);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ConfigAccessException("Cannot invoke getter " + getter, e);
        }
    }

    @Override
    public void set(Object owner, Object value) {
        try {
            setter.invoke(owner, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ConfigAccessException("Cannot invoke setter " + setter, e);
        }
    }

    @Override
    public Class<?> type() {
        return getter.getReturnType();
    }
}

/**
 * Record component access — read-only. Records are immutable, so a
 * "write" isn't a single-field operation; {@link FranklyConfigHolder}
 * rebuilds the whole record via its canonical constructor instead
 * (see {@link ConfigSchema#reconstructRecord}).
 */
final class RecordComponentAccessor implements ConfigAccessor {
    private final Method accessor;

    RecordComponentAccessor(Method accessor) {
        accessor.setAccessible(true);
        this.accessor = accessor;
    }

    @Override
    public Object get(Object owner) {
        try {
            return accessor.invoke(owner);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ConfigAccessException("Cannot read record component " + accessor, e);
        }
    }

    @Override
    public void set(Object owner, Object value) {
        throw new UnsupportedOperationException(
                "Record components are immutable; use FranklyConfigHolder.update(...) to replace the whole record");
    }

    @Override
    public Class<?> type() {
        return accessor.getReturnType();
    }
}
