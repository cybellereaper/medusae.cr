package com.github.cybellereaper.medusae.commands.core.response;

public record FollowupResponse(String content, boolean ephemeral) implements CommandResponse {
}
