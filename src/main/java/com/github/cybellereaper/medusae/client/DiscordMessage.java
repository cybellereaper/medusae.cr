package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable message payload abstraction used for both channel messages and interaction responses.
 */
public record DiscordMessage(
        String content,
        List<DiscordEmbed> embeds,
        List<DiscordActionRow> components,
        boolean ephemeral
) {
    private static final int EPHEMERAL_FLAG = 1 << 6;

    public DiscordMessage(String content, List<DiscordEmbed> embeds, boolean ephemeral) {
        this(content, embeds, List.of(), ephemeral);
    }

    public DiscordMessage {
        if (embeds == null || embeds.isEmpty()) {
            embeds = List.of();
        } else {
            embeds = embeds.stream().filter(Objects::nonNull).toList();
        }

        if (components == null || components.isEmpty()) {
            components = List.of();
        } else {
            components = components.stream().filter(Objects::nonNull).toList();
        }
    }

    public static DiscordMessage ofContent(String content) {
        return new DiscordMessage(content, List.of(), List.of(), false);
    }

    public static DiscordMessage ofEmbeds(String content, List<DiscordEmbed> embeds) {
        return new DiscordMessage(content, embeds, List.of(), false);
    }

    public static DiscordMessage ofComponents(String content, List<DiscordActionRow> components) {
        return new DiscordMessage(content, List.of(), components, false);
    }

    public DiscordMessage withComponents(List<DiscordActionRow> componentRows) {
        return new DiscordMessage(content, embeds, componentRows, ephemeral);
    }

    public DiscordMessage asEphemeral() {
        return ephemeral ? this : new DiscordMessage(content, embeds, components, true);
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (content != null && !content.isBlank()) {
            payload.put("content", content);
        }

        if (!embeds.isEmpty()) {
            payload.put("embeds", embeds.stream()
                    .map(DiscordEmbed::toPayload)
                    .filter(map -> !map.isEmpty())
                    .toList());
        }

        if (!components.isEmpty()) {
            payload.put("components", components.stream().map(DiscordActionRow::toPayload).toList());
        }

        if (ephemeral) {
            payload.put("flags", EPHEMERAL_FLAG);
        }

        return payload;
    }
}
