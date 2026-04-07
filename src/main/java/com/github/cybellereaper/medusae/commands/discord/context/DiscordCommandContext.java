package com.github.cybellereaper.medusae.commands.discord.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.medusae.client.InteractionContext;
import com.github.cybellereaper.medusae.commands.core.execute.CommandContext;

public record DiscordCommandContext(CommandContext core, InteractionContext interactionContext,
                                    JsonNode rawInteraction) {
}
