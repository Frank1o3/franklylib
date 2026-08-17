package com.frank1o3.franklylib.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigValueHandlersTest {

    @Test
    void serializesBoxedValuesFromPrimitiveFields() {
        JsonBackedPrimitiveConfig config = new JsonBackedPrimitiveConfig();

        var json = ConfigSchema.of(JsonBackedPrimitiveConfig.class).serialize(config);

        assertEquals(true, json.get("enabled").getAsBoolean());
        assertEquals(42, json.get("count").getAsInt());
        assertEquals(1.25d, json.get("amount").getAsDouble());
    }

    @Test
    void recordWithAllAnnotatedComponentsBuildsSchema() {
        ConfigSchema schema = ConfigSchema.of(AnnotatedRecordConfig.class);
        assertEquals(2, schema.entries().size());
        assertEquals(true, schema.isRecord());
    }

    @Test
    void recordWithMissingAnnotationThrowsException() {
        org.junit.jupiter.api.Assertions.assertThrows(ConfigSchemaException.class, () -> {
            ConfigSchema.of(PartiallyAnnotatedRecordConfig.class);
        });
    }

    private static final class JsonBackedPrimitiveConfig {
        @ConfigEntry
        private boolean enabled = true;

        @ConfigEntry
        private int count = 42;

        @ConfigEntry
        private double amount = 1.25d;
    }

    public record AnnotatedRecordConfig(@ConfigEntry int speed, @ConfigEntry String name) {
    }

    public record PartiallyAnnotatedRecordConfig(@ConfigEntry int speed, String name) {
    }
}
