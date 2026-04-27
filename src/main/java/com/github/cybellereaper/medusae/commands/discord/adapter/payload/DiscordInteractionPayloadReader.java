package com.github.cybellereaper.medusae.commands.discord.adapter.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DiscordInteractionPayloadReader {
    private final ObjectMapper objectMapper;

    public DiscordInteractionPayloadReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DiscordInteractionPayload read(String interaction) {
        if (interaction == null) {
            throw new NullPointerException("interaction");
        }
        try {
            return objectMapper.readValue(interaction, DiscordInteractionPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid Discord interaction payload", exception);
        }
    }
}
