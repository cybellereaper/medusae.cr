package com.github.cybellereaper.medusae.commands.discord.adapter;

import com.github.cybellereaper.medusae.client.AutocompleteChoice;
import com.github.cybellereaper.medusae.client.InteractionContext;
import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import com.github.cybellereaper.medusae.commands.core.execute.CommandResponder;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import com.github.cybellereaper.medusae.commands.core.response.CommandResponse;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;
import com.github.cybellereaper.medusae.commands.discord.response.DiscordResponseApplier;

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

    public void dispatch(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
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

    public void dispatchAutocomplete(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
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


    public void dispatchButton(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        dispatchComponent(interaction, interactionContext, InteractionHandlerType.BUTTON);
    }

    public void dispatchComponent(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        java.util.Objects.requireNonNull(interaction, "interaction");
        java.util.Objects.requireNonNull(interactionContext, "interactionContext");
        Integer componentTypeCode = mapper.componentType(interaction);
        InteractionHandlerType type = switch (componentTypeCode == null ? 0 : componentTypeCode) {
            case 2 -> InteractionHandlerType.BUTTON;
            case 3 -> InteractionHandlerType.STRING_SELECT;
            case 5 -> InteractionHandlerType.USER_SELECT;
            case 6 -> InteractionHandlerType.ROLE_SELECT;
            case 7 -> InteractionHandlerType.MENTIONABLE_SELECT;
            case 8 -> InteractionHandlerType.CHANNEL_SELECT;
            default -> null;
        };
        if (type != null) {
            dispatchComponent(interaction, interactionContext, type);
        }
    }

    public void dispatchStringSelect(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        dispatchComponent(interaction, interactionContext, InteractionHandlerType.STRING_SELECT);
    }

    public void dispatchUserSelect(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        dispatchComponent(interaction, interactionContext, InteractionHandlerType.USER_SELECT);
    }

    public void dispatchRoleSelect(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        dispatchComponent(interaction, interactionContext, InteractionHandlerType.ROLE_SELECT);
    }

    public void dispatchMentionableSelect(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        dispatchComponent(interaction, interactionContext, InteractionHandlerType.MENTIONABLE_SELECT);
    }

    public void dispatchChannelSelect(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        dispatchComponent(interaction, interactionContext, InteractionHandlerType.CHANNEL_SELECT);
    }

    public void dispatchModal(DiscordInteractionPayload interaction, InteractionContext interactionContext) {
        java.util.Objects.requireNonNull(interaction, "interaction");
        java.util.Objects.requireNonNull(interactionContext, "interactionContext");
        var coreInteraction = mapper.toModalInteraction(interaction, interactionContext);
        var trackingResponder = new TrackingResponder(new DiscordResponseApplier(interactionContext));

        try {
            framework.executeInteraction(coreInteraction, trackingResponder);
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

    private void dispatchComponent(DiscordInteractionPayload interaction, InteractionContext interactionContext, InteractionHandlerType type) {
        java.util.Objects.requireNonNull(interaction, "interaction");
        java.util.Objects.requireNonNull(interactionContext, "interactionContext");
        var coreInteraction = mapper.toComponentInteraction(interaction, interactionContext, type);
        var trackingResponder = new TrackingResponder(new DiscordResponseApplier(interactionContext));

        try {
            framework.executeInteraction(coreInteraction, trackingResponder);
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

    private static final class TrackingResponder implements CommandResponder {
        private final CommandResponder delegate;
        private boolean responded;

        private TrackingResponder(CommandResponder delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(CommandResponse response) {
            responded = true;
            delegate.accept(response);
        }

        private boolean responded() {
            return responded;
        }
    }
}
