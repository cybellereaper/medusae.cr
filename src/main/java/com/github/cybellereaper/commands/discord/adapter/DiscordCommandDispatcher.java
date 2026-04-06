package com.github.cybellereaper.commands.discord.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.client.AutocompleteChoice;
import com.github.cybellereaper.client.InteractionContext;
import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.core.execute.CommandResponder;
import com.github.cybellereaper.commands.discord.response.DiscordResponseApplier;

import java.util.List;

public final class DiscordCommandDispatcher {
    private static final String GENERIC_ERROR_MESSAGE = "Sorry, something went wrong while executing this command.";
    private static final String NO_RESPONSE_MESSAGE = "Command completed without sending a response.";

    private final CommandFramework framework;
    private final DiscordInteractionMapper mapper;

    public DiscordCommandDispatcher(CommandFramework framework) {
        this.framework = java.util.Objects.requireNonNull(framework, "framework");
        this.mapper = new DiscordInteractionMapper();
    }

    public void dispatch(JsonNode interaction, InteractionContext interactionContext) {
        java.util.Objects.requireNonNull(interaction, "interaction");
        java.util.Objects.requireNonNull(interactionContext, "interactionContext");
        var coreInteraction = mapper.toCoreInteraction(interaction, interactionContext);
        var trackingResponder = new TrackingResponder(new DiscordResponseApplier(interactionContext));

        try {
            framework.execute(coreInteraction, trackingResponder);
            if (!trackingResponder.responded()) {
                interactionContext.respondEphemeral(NO_RESPONSE_MESSAGE);
            }
        } catch (RuntimeException exception) {
            if (!trackingResponder.responded()) {
                interactionContext.respondEphemeral(GENERIC_ERROR_MESSAGE);
            }
            throw exception;
        }
    }

    public void dispatchAutocomplete(JsonNode interaction, InteractionContext interactionContext) {
        java.util.Objects.requireNonNull(interaction, "interaction");
        java.util.Objects.requireNonNull(interactionContext, "interactionContext");
        var coreInteraction = mapper.toCoreInteraction(interaction, interactionContext);
        try {
            List<String> suggestions = framework.executeAutocomplete(coreInteraction, new DiscordResponseApplier(interactionContext));
            List<AutocompleteChoice> choices = suggestions.stream().limit(25).map(value -> new AutocompleteChoice(value, value)).toList();
            interactionContext.respondWithAutocompleteChoices(choices);
        } catch (RuntimeException exception) {
            interactionContext.respondWithAutocompleteChoices(List.of());
            throw exception;
        }
    }

    private static final class TrackingResponder implements CommandResponder {
        private final CommandResponder delegate;
        private boolean responded;

        private TrackingResponder(CommandResponder delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(com.github.cybellereaper.commands.core.response.CommandResponse response) {
            responded = true;
            delegate.accept(response);
        }

        private boolean responded() {
            return responded;
        }
    }
}
