package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record DiscordSelectOption(
        String label,
        String value,
        String description,
        boolean isDefault
) {
    public DiscordSelectOption {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(value, "value");

        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static DiscordSelectOption of(String label, String value) {
        return new DiscordSelectOption(label, value, null, false);
    }

    public DiscordSelectOption withDescription(String description) {
        return new DiscordSelectOption(label, value, description, isDefault);
    }

    public DiscordSelectOption asDefault() {
        return isDefault ? this : new DiscordSelectOption(label, value, description, true);
    }

    Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("label", label);
        payload.put("value", value);
        if (description != null && !description.isBlank()) {
            payload.put("description", description);
        }
        if (isDefault) {
            payload.put("default", true);
        }
        return payload;
    }
}
