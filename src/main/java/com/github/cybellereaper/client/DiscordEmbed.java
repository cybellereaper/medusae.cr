package com.github.cybellereaper.client;

import java.util.LinkedHashMap;
import java.util.Map;

public record DiscordEmbed(
        String title,
        String description,
        Integer color,
        String url,
        String imageUrl,
        String thumbnailUrl
) {
    public DiscordEmbed(String title, String description, Integer color) {
        this(title, description, color, null, null, null);
    }

    public DiscordEmbed withImage(String imageUrl) {
        return new DiscordEmbed(title, description, color, url, imageUrl, thumbnailUrl);
    }

    public DiscordEmbed withThumbnail(String thumbnailUrl) {
        return new DiscordEmbed(title, description, color, url, imageUrl, thumbnailUrl);
    }

    public DiscordEmbed withUrl(String url) {
        return new DiscordEmbed(title, description, color, url, imageUrl, thumbnailUrl);
    }

    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();

        putIfText(payload, "title", title);
        putIfText(payload, "description", description);

        if (color != null) {
            payload.put("color", color);
        }

        putIfText(payload, "url", url);

        if (isText(imageUrl)) {
            payload.put("image", Map.of("url", imageUrl));
        }

        if (isText(thumbnailUrl)) {
            payload.put("thumbnail", Map.of("url", thumbnailUrl));
        }

        return payload;
    }

    private static void putIfText(Map<String, Object> payload, String key, String value) {
        if (isText(value)) {
            payload.put(key, value);
        }
    }

    private static boolean isText(String value) {
        return value != null && !value.isBlank();
    }
}
