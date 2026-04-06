package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedRole(
        String id,
        String name,
        int color
) {
    public static ResolvedRole from(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new ResolvedRole(
                node.path("id").asText(null),
                node.path("name").asText(null),
                node.path("color").asInt(0)
        );
    }
}
