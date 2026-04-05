package com.github.cybellereaper.client;

@FunctionalInterface
public interface InteractionHandler {
    void handle(InteractionContext interaction);
}
