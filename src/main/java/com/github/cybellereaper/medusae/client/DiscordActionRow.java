package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DiscordActionRow(List<DiscordComponent> components) {
    public DiscordActionRow {
        Objects.requireNonNull(components, "components");
        components = components.stream().filter(Objects::nonNull).toList();
        if (components.isEmpty()) {
            throw new IllegalArgumentException("components must not be empty");
        }
    }

    public static DiscordActionRow of(List<DiscordComponent> components) {
        return new DiscordActionRow(components);
    }

    Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", 1);
        payload.put("components", components.stream().map(DiscordComponent::toPayload).toList());
        return payload;
    }
}
