package com.github.cybellereaper.medusae.commands.discord.adapter.payload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class DiscordInteractionPayloadReader {
    private final ObjectMapper objectMapper;

    public DiscordInteractionPayloadReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DiscordInteractionPayload read(JsonNode interaction) {
        if (interaction == null) {
            throw new NullPointerException("interaction");
        }
        return objectMapper.convertValue(interaction, DiscordInteractionPayload.class);
    }
}
