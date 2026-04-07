package com.github.cybellereaper.medusae.commands.core.response;

public sealed interface CommandResponse permits ImmediateResponse, DeferredResponse, FollowupResponse, InteractionReply, ModalReply {
}
