package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedMember(
        String userId,
        String nickname
) {
    public static ResolvedMember from(String userId, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new ResolvedMember(
                userId,
                node.path("nick").asText(null)
        );
    }
}
