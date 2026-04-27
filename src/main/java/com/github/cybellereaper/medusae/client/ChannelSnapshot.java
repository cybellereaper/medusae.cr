package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChannelSnapshot(
        String id,
        String name,
        int type
) {
}
