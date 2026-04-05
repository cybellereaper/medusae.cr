package com.github.cybellereaper.client;

@FunctionalInterface
public interface SlashCommandHandler {
    void handle(SlashCommandInteraction interaction);
}
