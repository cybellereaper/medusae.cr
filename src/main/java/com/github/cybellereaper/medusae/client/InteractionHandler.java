package com.github.cybellereaper.medusae.client;

@FunctionalInterface
public interface InteractionHandler {
    void handle(InteractionContext context);
}
