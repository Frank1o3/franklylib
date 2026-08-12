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

    private static final class JsonBackedPrimitiveConfig {
        @ConfigEntry
        private boolean enabled = true;

        @ConfigEntry
        private int count = 42;

        @ConfigEntry
        private double amount = 1.25d;
    }
}
