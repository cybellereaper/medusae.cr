package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record DiscordTextInput(
        String customId,
        int style,
        String label,
        Integer minLength,
        Integer maxLength,
        boolean required,
        String value,
        String placeholder
) implements DiscordComponent {
    public static final int SHORT = 1;
    public static final int PARAGRAPH = 2;

    public DiscordTextInput {
        requireNonBlank(customId, "customId");
        requireNonBlank(label, "label");

        if (style < SHORT || style > PARAGRAPH) {
            throw new IllegalArgumentException("Unsupported text input style: " + style);
        }

        if (minLength != null && minLength < 0) {
            throw new IllegalArgumentException("minLength must be >= 0");
        }

        if (maxLength != null && maxLength < 1) {
            throw new IllegalArgumentException("maxLength must be >= 1");
        }

        if (minLength != null && maxLength != null && minLength > maxLength) {
            throw new IllegalArgumentException("minLength cannot be greater than maxLength");
        }
    }

    public static DiscordTextInput shortInput(String customId, String label) {
        return new DiscordTextInput(customId, SHORT, label, null, null, true, null, null);
    }

    public static DiscordTextInput paragraph(String customId, String label) {
        return new DiscordTextInput(customId, PARAGRAPH, label, null, null, true, null, null);
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public DiscordTextInput withLengthRange(Integer minLength, Integer maxLength) {
        return new DiscordTextInput(customId, style, label, minLength, maxLength, required, value, placeholder);
    }

    public DiscordTextInput optional() {
        return required ? new DiscordTextInput(customId, style, label, minLength, maxLength, false, value, placeholder) : this;
    }

    public DiscordTextInput withValue(String value) {
        return new DiscordTextInput(customId, style, label, minLength, maxLength, required, value, placeholder);
    }

    public DiscordTextInput withPlaceholder(String placeholder) {
        return new DiscordTextInput(customId, style, label, minLength, maxLength, required, value, placeholder);
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", 4);
        payload.put("custom_id", customId);
        payload.put("style", style);
        payload.put("label", label);

        if (minLength != null) {
            payload.put("min_length", minLength);
        }
        if (maxLength != null) {
            payload.put("max_length", maxLength);
        }
        payload.put("required", required);

        if (value != null) {
            payload.put("value", value);
        }
        if (placeholder != null && !placeholder.isBlank()) {
            payload.put("placeholder", placeholder);
        }

        return payload;
    }
}
