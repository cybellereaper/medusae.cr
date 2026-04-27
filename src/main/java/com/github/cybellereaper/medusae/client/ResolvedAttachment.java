package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResolvedAttachment(
        String id,
        String filename,
        @JsonProperty("content_type")
        String contentType,
        long size,
        String url,
        @JsonProperty("proxy_url")
        String proxyUrl
) {
}
