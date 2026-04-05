package com.github.cybellereaper.client;

/**
 * Context passed to typed modal submit handlers.
 */
public record ModalSubmitInteraction(InteractionContext context, ModalSubmitParameters parameters) {
}
