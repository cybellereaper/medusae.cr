package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

final class InteractionOptionReader {
    static final String OPTIONS_FIELD = "options";

    private static final String NAME_FIELD = "name";
    private static final String VALUE_FIELD = "value";

    private final JsonNode options;

    InteractionOptionReader(JsonNode options) {
        this.options = Objects.requireNonNull(options, "options");
    }

    String optionString(String optionName) {
        JsonNode option = findOptionNode(optionName);
        return option == null ? null : textOrNull(option.path(VALUE_FIELD));
    }

    Long optionLong(String optionName) {
        JsonNode value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value.isIntegralNumber()) {
            return value.longValue();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    Integer optionInt(String optionName) {
        Long value = optionLong(optionName);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    Boolean optionBoolean(String optionName) {
        JsonNode value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            String text = value.asText().trim();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return null;
    }

    Double optionDouble(String optionName) {
        JsonNode value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    String optionResolvedId(String optionName) {
        JsonNode value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value.isTextual()) {
            String text = value.asText().trim();
            return text.isEmpty() ? null : text;
        }
        if (value.isIntegralNumber()) {
            return Long.toString(value.longValue());
        }
        return null;
    }

    private JsonNode optionValue(String optionName) {
        JsonNode option = findOptionNode(optionName);
        return option == null ? null : option.path(VALUE_FIELD);
    }

    private JsonNode findOptionNode(String optionName) {
        Objects.requireNonNull(optionName, "optionName");
        return findOptionNode(optionName, options);
    }

    private static JsonNode findOptionNode(String optionName, JsonNode candidateOptions) {
        if (!candidateOptions.isArray()) {
            return null;
        }
        for (JsonNode option : candidateOptions) {
            if (optionName.equals(option.path(NAME_FIELD).asText())) {
                return option;
            }
            JsonNode nested = findOptionNode(optionName, option.path(OPTIONS_FIELD));
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    static String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isBlank() ? null : text;
    }
}
