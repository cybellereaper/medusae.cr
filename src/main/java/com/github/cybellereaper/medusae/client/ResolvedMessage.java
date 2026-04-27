package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResolvedMessage(String id, String content, String authorId) {
    public ResolvedMessage(String id, String content, ResolvedUser author) {
        this(id, content, author == null ? null : author.id());
    }
}
