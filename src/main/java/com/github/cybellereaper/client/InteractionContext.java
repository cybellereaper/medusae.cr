package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Immutable wrapper around a raw Discord interaction payload that exposes
 * common contextual metadata.
 */
public record InteractionContext(JsonNode payload) {
    public InteractionContext {
        Objects.requireNonNull(payload, "payload");
    }

    public String id() {
        return textValue(payload.path("id"));
    }

    public String token() {
        return textValue(payload.path("token"));
    }

    public int type() {
        return payload.path("type").asInt();
    }

    public String guildId() {
        return textValue(payload.path("guild_id"));
    }

    public String channelId() {
        return textValue(payload.path("channel_id"));
    }

    public String commandName() {
        return textValue(payload.path("data").path("name"));
    }

    public String customId() {
        return textValue(payload.path("data").path("custom_id"));
    }

    public String userId() {
        JsonNode userNode = resolveUserNode();
        return textValue(userNode.path("id"));
    }

    public String username() {
        JsonNode userNode = resolveUserNode();
        return textValue(userNode.path("username"));
    }

    public JsonNode raw() {
        return payload;
    }

    private JsonNode resolveUserNode() {
        JsonNode memberUser = payload.path("member").path("user");
        return memberUser.isMissingNode() ? payload.path("user") : memberUser;
    }

    private static String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isBlank() ? null : text;
    }
}
