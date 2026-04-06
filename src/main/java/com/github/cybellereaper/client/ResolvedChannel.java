package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedChannel(
        String id,
        String name,
        int type
) {
    public static ResolvedChannel from(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new ResolvedChannel(
                node.path("id").asText(null),
                node.path("name").asText(null),
                node.path("type").asInt(0)
        );
    }
}
