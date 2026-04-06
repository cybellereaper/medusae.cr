package com.github.cybellereaper.commands.core.response;

public sealed interface CommandResponse permits ImmediateResponse, DeferredResponse, FollowupResponse {
}
