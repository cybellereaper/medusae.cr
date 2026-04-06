package com.github.cybellereaper.commands.discord.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.client.InteractionContext;
import com.github.cybellereaper.commands.core.execute.CommandContext;

public record DiscordCommandContext(CommandContext core, InteractionContext interactionContext, JsonNode rawInteraction) {
}
