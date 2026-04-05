package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed slash-command option set with typed accessors.
 */
public final class SlashCommandParameters {
    private final Map<String, JsonNode> optionsByName;

    SlashCommandParameters(JsonNode interaction) {
        Objects.requireNonNull(interaction, "interaction");
        this.optionsByName = readOptions(interaction.path("data").path("options"));
    }

    public String getString(String optionName) {
        JsonNode value = getOptionNode(optionName);
        return value == null ? null : value.asText();
    }

    public Long getLong(String optionName) {
        JsonNode value = getOptionNode(optionName);
        if (value == null || !value.isNumber()) {
            return null;
        }
        return value.longValue();
    }

    public Double getDouble(String optionName) {
        JsonNode value = getOptionNode(optionName);
        if (value == null || !value.isNumber()) {
            return null;
        }
        return value.doubleValue();
    }

    public Boolean getBoolean(String optionName) {
        JsonNode value = getOptionNode(optionName);
        if (value == null || !value.isBoolean()) {
            return null;
        }
        return value.booleanValue();
    }

    public String requireString(String optionName) {
        String value = getString(optionName);
        if (value == null) {
            throw new IllegalArgumentException("Missing required string option: " + optionName);
        }
        return value;
    }

    private JsonNode getOptionNode(String optionName) {
        Objects.requireNonNull(optionName, "optionName");
        if (optionName.isBlank()) {
            throw new IllegalArgumentException("optionName must not be blank");
        }
        return optionsByName.get(optionName);
    }

    private static Map<String, JsonNode> readOptions(JsonNode optionsNode) {
        if (!optionsNode.isArray()) {
            return Map.of();
        }

        Map<String, JsonNode> optionMap = new HashMap<>();
        for (JsonNode option : optionsNode) {
            String optionName = option.path("name").asText("");
            if (optionName.isBlank()) {
                continue;
            }

            JsonNode value = option.path("value");
            if (!value.isMissingNode() && !value.isNull()) {
                optionMap.put(optionName, value);
            }
        }

        return Map.copyOf(optionMap);
    }
}
