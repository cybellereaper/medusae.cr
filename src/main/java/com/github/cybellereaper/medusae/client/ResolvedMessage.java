package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedMessage(String id, String content, String authorId) {
    public static ResolvedMessage from(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new ResolvedMessage(
                node.path("id").asText(null),
                node.path("content").asText(null),
                node.path("author").path("id").asText(null)
        );
    }
}
