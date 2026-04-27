package com.github.cybellereaper.medusae.commands.discord.context;

import com.github.cybellereaper.medusae.client.InteractionContext;
import com.github.cybellereaper.medusae.commands.core.execute.CommandContext;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;

public record DiscordCommandContext(CommandContext core, InteractionContext interactionContext,
                                    DiscordInteractionPayload rawInteraction) {
}
