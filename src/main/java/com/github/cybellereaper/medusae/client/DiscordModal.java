package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DiscordModal(String customId, String title, List<DiscordActionRow> components) {
    public DiscordModal {
        requireNonBlank(customId, "customId");
        requireNonBlank(title, "title");

        Objects.requireNonNull(components, "components");
        components = components.stream().filter(Objects::nonNull).toList();
        if (components.isEmpty()) {
            throw new IllegalArgumentException("components must not be empty");
        }

        boolean hasInvalidComponent = components.stream()
                .flatMap(row -> row.components().stream())
                .anyMatch(component -> !(component instanceof DiscordTextInput));

        if (hasInvalidComponent) {
            throw new IllegalArgumentException("modal components can only contain text inputs");
        }
    }

    public static DiscordModal of(String customId, String title, List<DiscordActionRow> components) {
        return new DiscordModal(customId, title, components);
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("custom_id", customId);
        payload.put("title", title);
        payload.put("components", components.stream().map(DiscordActionRow::toPayload).toList());
        return payload;
    }
}
