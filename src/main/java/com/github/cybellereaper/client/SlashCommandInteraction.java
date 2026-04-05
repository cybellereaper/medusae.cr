package com.github.cybellereaper.client;

/**
 * Context passed to typed slash-command and autocomplete handlers.
 */
public record SlashCommandInteraction(InteractionContext context, SlashCommandParameters parameters) {
}
