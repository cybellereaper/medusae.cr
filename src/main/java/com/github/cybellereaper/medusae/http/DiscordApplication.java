package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordApplication(String id, String name) {
}
