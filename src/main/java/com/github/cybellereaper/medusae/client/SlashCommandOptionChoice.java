package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record SlashCommandOptionChoice(String name, Object value) {
    public SlashCommandOptionChoice {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (!(value instanceof String || value instanceof Integer || value instanceof Long || value instanceof Double)) {
            throw new IllegalArgumentException("value must be a String, Integer, Long, or Double");
        }
    }

    public Map<String, Object> toRequestPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("value", value);
        return payload;
    }
}
