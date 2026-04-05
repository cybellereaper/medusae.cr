package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class SlashCommandRouter {
    private static final int PING_INTERACTION_TYPE = 1;
    private static final int APPLICATION_COMMAND_INTERACTION_TYPE = 2;

    private static final int CHAT_INPUT_COMMAND_TYPE = 1;
    private static final int USER_CONTEXT_COMMAND_TYPE = 2;
    private static final int MESSAGE_CONTEXT_COMMAND_TYPE = 3;
    private static final int MESSAGE_COMPONENT_INTERACTION_TYPE = 3;
    private static final int APPLICATION_COMMAND_AUTOCOMPLETE_INTERACTION_TYPE = 4;
    private static final int MODAL_SUBMIT_INTERACTION_TYPE = 5;

    private static final int PONG_RESPONSE_TYPE = 1;
    private static final int CHANNEL_MESSAGE_RESPONSE_TYPE = 4;
    private static final int DEFERRED_CHANNEL_MESSAGE_RESPONSE_TYPE = 5;
    private static final int DEFERRED_MESSAGE_UPDATE_RESPONSE_TYPE = 6;
    private static final int AUTOCOMPLETE_RESPONSE_TYPE = 8;
    private static final int MODAL_RESPONSE_TYPE = 9;

    private final Map<String, Consumer<JsonNode>> slashHandlers = new ConcurrentHashMap<>();
    private final Map<String, SlashCommandHandler> slashContextHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> userContextMenuHandlers = new ConcurrentHashMap<>();
    private final Map<String, InteractionHandler> userContextMenuContextHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> messageContextMenuHandlers = new ConcurrentHashMap<>();
    private final Map<String, InteractionHandler> messageContextMenuContextHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> componentHandlers = new ConcurrentHashMap<>();
    private final Map<String, InteractionHandler> componentContextHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> modalHandlers = new ConcurrentHashMap<>();
    private final Map<String, ModalSubmitHandler> modalContextHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<JsonNode>> autocompleteHandlers = new ConcurrentHashMap<>();
    private final Map<String, SlashCommandHandler> autocompleteContextHandlers = new ConcurrentHashMap<>();
    private final InteractionResponder responder;

    SlashCommandRouter(InteractionResponder responder) {
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    void registerSlashHandler(String commandName, Consumer<JsonNode> handler) {
        registerUniqueHandler(slashHandlers, commandName, "slash command", handler);
    }

    void registerSlashContextHandler(String commandName, SlashCommandHandler handler) {
        registerUniqueHandler(slashContextHandlers, commandName, "slash command", handler);
    }

    void registerComponentHandler(String customId, Consumer<JsonNode> handler) {
        registerUniqueHandler(componentHandlers, customId, "component", handler);
    }

    void registerComponentContextHandler(String customId, InteractionHandler handler) {
        registerUniqueHandler(componentContextHandlers, customId, "component", handler);
    }

    void registerModalHandler(String customId, Consumer<JsonNode> handler) {
        registerUniqueHandler(modalHandlers, customId, "modal", handler);
    }

    void registerModalContextHandler(String customId, ModalSubmitHandler handler) {
        registerUniqueHandler(modalContextHandlers, customId, "modal", handler);
    }

    void registerAutocompleteHandler(String commandName, Consumer<JsonNode> handler) {
        registerUniqueHandler(autocompleteHandlers, commandName, "autocomplete", handler);
    }

    void registerAutocompleteContextHandler(String commandName, SlashCommandHandler handler) {
        registerUniqueHandler(autocompleteContextHandlers, commandName, "autocomplete", handler);
    }

    void registerUserContextMenuHandler(String commandName, Consumer<JsonNode> handler) {
        registerUniqueHandler(userContextMenuHandlers, commandName, "user context menu", handler);
    }

    void registerUserContextMenuContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(userContextMenuContextHandlers, commandName, "user context menu", handler);
    }

    void registerMessageContextMenuHandler(String commandName, Consumer<JsonNode> handler) {
        registerUniqueHandler(messageContextMenuHandlers, commandName, "message context menu", handler);
    }

    void registerMessageContextMenuContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(messageContextMenuContextHandlers, commandName, "message context menu", handler);
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
            int commandType = interaction.path("data").path("type").asInt(CHAT_INPUT_COMMAND_TYPE);
            if (commandType == USER_CONTEXT_COMMAND_TYPE) {
                dispatchRawAndContextByName(
                        interaction,
                        userContextMenuHandlers,
                        userContextMenuContextHandlers,
                        "name",
                        this::toInteractionContext,
                        InteractionHandler::handle
                );
            } else if (commandType == MESSAGE_CONTEXT_COMMAND_TYPE) {
                dispatchRawAndContextByName(
                        interaction,
                        messageContextMenuHandlers,
                        messageContextMenuContextHandlers,
                        "name",
                        this::toInteractionContext,
                        InteractionHandler::handle
                );
            } else {
                dispatchRawAndContextByName(
                        interaction,
                        slashHandlers,
                        slashContextHandlers,
                        "name",
                        this::toSlashCommandInteraction,
                        SlashCommandHandler::handle
                );
            }
            return;
        }

        if (interactionType == APPLICATION_COMMAND_AUTOCOMPLETE_INTERACTION_TYPE) {
            dispatchRawAndContextByName(
                    interaction,
                    autocompleteHandlers,
                    autocompleteContextHandlers,
                    "name",
                    this::toSlashCommandInteraction,
                    SlashCommandHandler::handle
            );
            return;
        }

        if (interactionType == MESSAGE_COMPONENT_INTERACTION_TYPE || interactionType == MODAL_SUBMIT_INTERACTION_TYPE) {
            if (interactionType == MESSAGE_COMPONENT_INTERACTION_TYPE) {
                dispatchRawAndContextByName(
                        interaction,
                        componentHandlers,
                        componentContextHandlers,
                        "custom_id",
                        this::toInteractionContext,
                        InteractionHandler::handle
                );
            } else {
                dispatchRawAndContextByName(
                        interaction,
                        modalHandlers,
                        modalContextHandlers,
                        "custom_id",
                        this::toModalSubmitInteraction,
                        ModalSubmitHandler::handle
                );
            }
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

    void respondWithModal(JsonNode interaction, DiscordModal modal) {
        Objects.requireNonNull(modal, "modal");
        respond(interaction, MODAL_RESPONSE_TYPE, modal.toPayload());
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

    String getModalValue(JsonNode interaction, String customId) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(customId, "customId");

        JsonNode rows = interaction.path("data").path("components");
        if (!rows.isArray()) {
            return null;
        }

        for (JsonNode row : rows) {
            JsonNode components = row.path("components");
            if (!components.isArray()) {
                continue;
            }

            for (JsonNode component : components) {
                if (customId.equals(component.path("custom_id").asText())) {
                    JsonNode value = component.path("value");
                    return value.isMissingNode() || value.isNull() ? null : value.asText();
                }
            }
        }

        return null;
    }

    private <H, C> void dispatchRawAndContextByName(
            JsonNode interaction,
            Map<String, Consumer<JsonNode>> rawHandlers,
            Map<String, H> contextHandlers,
            String keyFieldName,
            Function<JsonNode, C> contextFactory,
            BiConsumer<H, C> contextInvoker
    ) {
        String handlerKey = interaction.path("data").path(keyFieldName).asText("");
        Consumer<JsonNode> rawHandler = rawHandlers.get(handlerKey);
        if (rawHandler != null) {
            rawHandler.accept(interaction);
        }

        H contextHandler = contextHandlers.get(handlerKey);
        if (contextHandler == null) {
            return;
        }

        C context = contextFactory.apply(interaction);
        contextInvoker.accept(contextHandler, context);
    }

    private InteractionContext toInteractionContext(JsonNode interaction) {
        return new InteractionContext(interaction);
    }

    private SlashCommandInteraction toSlashCommandInteraction(JsonNode interaction) {
        return new SlashCommandInteraction(new InteractionContext(interaction), new SlashCommandParameters(interaction));
    }

    private ModalSubmitInteraction toModalSubmitInteraction(JsonNode interaction) {
        return new ModalSubmitInteraction(new InteractionContext(interaction), new ModalSubmitParameters(interaction));
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

    private static <H> void registerUniqueHandler(
            Map<String, H> handlers,
            String key,
            String handlerType,
            H handler
    ) {
        validateKey(key, handlerType);
        Objects.requireNonNull(handler, "handler");

        H previous = handlers.putIfAbsent(key, handler);
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
