package com.github.cybellereaper.medusae.commands.core.response;

public record ImmediateResponse(String content, boolean ephemeral) implements CommandResponse {
    public static ImmediateResponse publicMessage(String content) {
        return new ImmediateResponse(content, false);
    }

    public static ImmediateResponse ephemeralMessage(String content) {
        return new ImmediateResponse(content, true);
    }
}
