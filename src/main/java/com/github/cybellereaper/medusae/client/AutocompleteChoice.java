package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record AutocompleteChoice(String name, String value) {
    public AutocompleteChoice {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");

        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", name);
        payload.put("value", value);
        return payload;
    }
}
