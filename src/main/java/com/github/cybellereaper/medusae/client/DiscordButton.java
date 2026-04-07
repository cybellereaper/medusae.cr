package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record DiscordButton(
        int style,
        String label,
        String customId,
        String url,
        String emoji,
        boolean disabled
) implements DiscordComponent {
    public static final int PRIMARY = 1;
    public static final int SECONDARY = 2;
    public static final int SUCCESS = 3;
    public static final int DANGER = 4;
    public static final int LINK = 5;

    public DiscordButton {
        Objects.requireNonNull(label, "label");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }

        if (style < PRIMARY || style > LINK) {
            throw new IllegalArgumentException("Unsupported button style: " + style);
        }

        boolean hasCustomId = customId != null && !customId.isBlank();
        boolean hasUrl = url != null && !url.isBlank();

        if (style == LINK) {
            if (!hasUrl || hasCustomId) {
                throw new IllegalArgumentException("Link buttons require url and cannot set customId");
            }
        } else {
            if (!hasCustomId || hasUrl) {
                throw new IllegalArgumentException("Non-link buttons require customId and cannot set url");
            }
        }
    }

    public static DiscordButton primary(String customId, String label) {
        return new DiscordButton(PRIMARY, label, customId, null, null, false);
    }

    public static DiscordButton link(String url, String label) {
        return new DiscordButton(LINK, label, null, url, null, false);
    }

    public DiscordButton withEmoji(String emojiName) {
        return new DiscordButton(style, label, customId, url, emojiName, disabled);
    }

    public DiscordButton disable() {
        return disabled ? this : new DiscordButton(style, label, customId, url, emoji, true);
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", 2);
        payload.put("style", style);
        payload.put("label", label);

        if (customId != null && !customId.isBlank()) {
            payload.put("custom_id", customId);
        }
        if (url != null && !url.isBlank()) {
            payload.put("url", url);
        }
        if (emoji != null && !emoji.isBlank()) {
            payload.put("emoji", Map.of("name", emoji));
        }
        if (disabled) {
            payload.put("disabled", true);
        }

        return payload;
    }
}
