package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed modal submit component values keyed by component custom id.
 */
public final class ModalSubmitParameters {
    private final Map<String, String> valuesByCustomId;

    ModalSubmitParameters(JsonNode interaction) {
        Objects.requireNonNull(interaction, "interaction");
        this.valuesByCustomId = readValues(interaction.path("data").path("components"));
    }

    public String getString(String customId) {
        validateCustomId(customId);
        return valuesByCustomId.get(customId);
    }

    public String requireString(String customId) {
        String value = getString(customId);
        if (value == null) {
            throw new IllegalArgumentException("Missing required modal value: " + customId);
        }
        return value;
    }

    private static Map<String, String> readValues(JsonNode rows) {
        if (!rows.isArray()) {
            return Map.of();
        }

        Map<String, String> values = new HashMap<>();
        for (JsonNode row : rows) {
            JsonNode components = row.path("components");
            if (!components.isArray()) {
                continue;
            }

            for (JsonNode component : components) {
                String customId = component.path("custom_id").asText("");
                if (customId.isBlank()) {
                    continue;
                }

                JsonNode value = component.path("value");
                if (!value.isMissingNode() && !value.isNull()) {
                    values.put(customId, value.asText());
                }
            }
        }

        return Map.copyOf(values);
    }

    private static void validateCustomId(String customId) {
        Objects.requireNonNull(customId, "customId");
        if (customId.isBlank()) {
            throw new IllegalArgumentException("customId must not be blank");
        }
    }
}
