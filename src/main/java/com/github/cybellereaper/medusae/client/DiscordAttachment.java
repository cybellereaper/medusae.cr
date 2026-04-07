package com.github.cybellereaper.medusae.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public record DiscordAttachment(String fileName, String contentType, byte[] content) {
    public DiscordAttachment {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(content, "content");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
    }

    public static DiscordAttachment fromPath(Path path) {
        Objects.requireNonNull(path, "path");
        String fileName = path.getFileName().toString();
        try {
            String contentType = Files.probeContentType(path);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return new DiscordAttachment(fileName, contentType, Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read attachment from " + path, exception);
        }
    }
}
