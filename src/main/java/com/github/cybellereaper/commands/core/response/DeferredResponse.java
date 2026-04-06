package com.github.cybellereaper.commands.core.response;

public record DeferredResponse(boolean ephemeral) implements CommandResponse {
}
