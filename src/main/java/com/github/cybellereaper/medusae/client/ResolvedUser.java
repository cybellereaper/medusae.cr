package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedUser(
        String id,
        String username,
        String globalName,
        boolean bot
) {
    public static ResolvedUser from(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new ResolvedUser(
                node.path("id").asText(null),
                node.path("username").asText(null),
                node.path("global_name").asText(null),
                node.path("bot").asBoolean(false)
        );
    }
}
