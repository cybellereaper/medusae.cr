package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

final class SlashCommandRouter {
    private static final int PING_INTERACTION_TYPE = 1;
    private static final int APPLICATION_COMMAND_INTERACTION_TYPE = 2;
    private static final int MESSAGE_COMPONENT_INTERACTION_TYPE = 3;
    private static final int APPLICATION_COMMAND_AUTOCOMPLETE_INTERACTION_TYPE = 4;
    private static final int MODAL_SUBMIT_INTERACTION_TYPE = 5;

    private static final int PONG_RESPONSE_TYPE = 1;
    private static final int CHANNEL_MESSAGE_RESPONSE_TYPE = 4;
    private static final int DEFERRED_CHANNEL_MESSAGE_RESPONSE_TYPE = 5;
    private static final int DEFERRED_MESSAGE_UPDATE_RESPONSE_TYPE = 6;
    private static final int AUTOCOMPLETE_RESPONSE_TYPE = 8;

    private final Map<String, Consumer<JsonNode>> slashHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> componentHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> modalHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> autocompleteHandlers = new ConcurrentHashMap<>();
    private final InteractionResponder responder;

    SlashCommandRouter(InteractionResponder responder) {
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    void registerSlashHandler(String commandName, Consumer<JsonNode> handler) {
        registerUniqueHandler(slashHandlers, commandName, "slash command", handler);
    }

    void registerComponentHandler(String customId, Consumer<JsonNode> handler) {
        registerUniqueHandler(componentHandlers, customId, "component", handler);
    }

    void registerModalHandler(String customId, Consumer<JsonNode> handler) {
        registerUniqueHandler(modalHandlers, customId, "modal", handler);
    }

    void registerAutocompleteHandler(String commandName, Consumer<JsonNode> handler) {
        registerUniqueHandler(autocompleteHandlers, commandName, "autocomplete", handler);
    }

    void handleInteraction(JsonNode interaction) {
        if (interaction == null) {
            return;
        }

        int interactionType = interaction.path("type").asInt();
        if (interactionType == PING_INTERACTION_TYPE) {
            respond(interaction, PONG_RESPONSE_TYPE, null);
            return;
        }

        if (interactionType == APPLICATION_COMMAND_INTERACTION_TYPE) {
            dispatchByName(interaction, slashHandlers, "name");
            return;
        }

        if (interactionType == APPLICATION_COMMAND_AUTOCOMPLETE_INTERACTION_TYPE) {
            dispatchByName(interaction, autocompleteHandlers, "name");
            return;
        }

        if (interactionType == MESSAGE_COMPONENT_INTERACTION_TYPE || interactionType == MODAL_SUBMIT_INTERACTION_TYPE) {
            dispatchByName(
                    interaction,
                    interactionType == MESSAGE_COMPONENT_INTERACTION_TYPE ? componentHandlers : modalHandlers,
                    "custom_id"
            );
        }
    }

    void respondWithMessage(JsonNode interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content));
    }

    void respondWithMessage(JsonNode interaction, DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(interaction, CHANNEL_MESSAGE_RESPONSE_TYPE, message.toPayload());
    }

    void respondWithEmbeds(JsonNode interaction, String content, List<DiscordEmbed> embeds) {
        respondWithMessage(interaction, DiscordMessage.ofEmbeds(content, embeds));
    }

    void respondEphemeral(JsonNode interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content).asEphemeral());
    }

    void respondEphemeralWithEmbeds(JsonNode interaction, String content, List<DiscordEmbed> embeds) {
        respondWithMessage(interaction, DiscordMessage.ofEmbeds(content, embeds).asEphemeral());
    }

    void respondWithAutocompleteChoices(JsonNode interaction, List<AutocompleteChoice> choices) {
        Objects.requireNonNull(choices, "choices");

        respond(interaction, AUTOCOMPLETE_RESPONSE_TYPE, Map.of(
                "choices", choices.stream().map(AutocompleteChoice::toPayload).toList()
        ));
    }

    void deferMessage(JsonNode interaction) {
        respond(interaction, DEFERRED_CHANNEL_MESSAGE_RESPONSE_TYPE, null);
    }

    void deferUpdate(JsonNode interaction) {
        respond(interaction, DEFERRED_MESSAGE_UPDATE_RESPONSE_TYPE, null);
    }

    String getOptionString(JsonNode interaction, String optionName) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(optionName, "optionName");

        JsonNode options = interaction.path("data").path("options");
        if (!options.isArray()) {
            return null;
        }

        for (JsonNode option : options) {
            if (optionName.equals(option.path("name").asText())) {
                JsonNode value = option.path("value");
                return value.isMissingNode() || value.isNull() ? null : value.asText();
            }
        }

        return null;
    }

    private void dispatchByName(JsonNode interaction, Map<String, Consumer<JsonNode>> handlers, String keyFieldName) {
        String handlerKey = interaction.path("data").path(keyFieldName).asText("");
        Consumer<JsonNode> handler = handlers.get(handlerKey);
        if (handler != null) {
            handler.accept(interaction);
        }
    }

    private void respond(JsonNode interaction, int responseType, Map<String, Object> data) {
        Objects.requireNonNull(interaction, "interaction");

        String interactionId = interaction.path("id").asText();
        String interactionToken = interaction.path("token").asText();

        if (interactionId.isBlank() || interactionToken.isBlank()) {
            throw new IllegalArgumentException("interaction must include id and token");
        }

        responder.respond(interactionId, interactionToken, responseType, data);
    }

    private static void registerUniqueHandler(
            Map<String, Consumer<JsonNode>> handlers,
            String key,
            String handlerType,
            Consumer<JsonNode> handler
    ) {
        validateKey(key, handlerType);
        Objects.requireNonNull(handler, "handler");

        Consumer<JsonNode> previous = handlers.putIfAbsent(key, handler);
        if (previous != null) {
            throw new IllegalArgumentException("Interaction handler already registered for " + handlerType + ": " + key);
        }
    }

    private static void validateKey(String key, String keyType) {
        Objects.requireNonNull(key, keyType + " key");
        if (key.isBlank()) {
            throw new IllegalArgumentException(keyType + " key must not be blank");
        }
    }

    @FunctionalInterface
    interface InteractionResponder {
        void respond(String interactionId, String interactionToken, int type, Map<String, Object> data);
    }
}
