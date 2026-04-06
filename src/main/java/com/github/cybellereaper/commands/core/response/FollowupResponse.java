package com.github.cybellereaper.commands.core.response;

public record FollowupResponse(String content, boolean ephemeral) implements CommandResponse {
}
