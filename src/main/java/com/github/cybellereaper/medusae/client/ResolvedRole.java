package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResolvedRole(
        String id,
        String name,
        int color
) {
}
