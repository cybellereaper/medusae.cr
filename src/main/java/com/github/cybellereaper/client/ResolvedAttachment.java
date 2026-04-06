package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

public record ResolvedAttachment(
        String id,
        String filename,
        String contentType,
        long size,
        String url,
        String proxyUrl
) {
    public static ResolvedAttachment from(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new ResolvedAttachment(
                node.path("id").asText(null),
                node.path("filename").asText(null),
                node.path("content_type").asText(null),
                node.path("size").asLong(0L),
                node.path("url").asText(null),
                node.path("proxy_url").asText(null)
        );
    }
}
