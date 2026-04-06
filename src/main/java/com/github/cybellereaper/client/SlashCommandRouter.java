package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

final class SlashCommandRouter {
    private static final String DATA_FIELD = "data";
    private static final String ID_FIELD = "id";
    private static final String TOKEN_FIELD = "token";

    private final Map<HandlerGroup, Map<String, Consumer<InteractionContext>>> handlerRegistry;
    private final InteractionResponder responder;

    SlashCommandRouter(InteractionResponder responder) {
        this.responder = Objects.requireNonNull(responder, "responder");
        this.handlerRegistry = initializeHandlerRegistry();
    }

    void registerSlashHandler(String commandName, Consumer<JsonNode> handler) {
        Objects.requireNonNull(handler, "handler");
        registerSlashContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerComponentHandler(String customId, Consumer<JsonNode> handler) {
        Objects.requireNonNull(handler, "handler");
        registerComponentContextHandler(customId, context -> handler.accept(context.raw()));
    }

    void registerModalHandler(String customId, Consumer<JsonNode> handler) {
        Objects.requireNonNull(handler, "handler");
        registerModalContextHandler(customId, context -> handler.accept(context.raw()));
    }

    void registerAutocompleteHandler(String commandName, Consumer<JsonNode> handler) {
        Objects.requireNonNull(handler, "handler");
        registerAutocompleteContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerUserContextMenuHandler(String commandName, Consumer<JsonNode> handler) {
        Objects.requireNonNull(handler, "handler");
        registerUserContextMenuContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerMessageContextMenuHandler(String commandName, Consumer<JsonNode> handler) {
        Objects.requireNonNull(handler, "handler");
        registerMessageContextMenuContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerSlashContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(HandlerGroup.SLASH_COMMAND, commandName, handler);
    }

    void registerComponentContextHandler(String customId, InteractionHandler handler) {
        registerUniqueHandler(HandlerGroup.COMPONENT, customId, handler);
    }

    void registerModalContextHandler(String customId, InteractionHandler handler) {
        registerUniqueHandler(HandlerGroup.MODAL, customId, handler);
    }

    void registerAutocompleteContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(HandlerGroup.AUTOCOMPLETE, commandName, handler);
    }

    void registerUserContextMenuContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(HandlerGroup.USER_CONTEXT_MENU, commandName, handler);
    }

    void registerMessageContextMenuContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(HandlerGroup.MESSAGE_CONTEXT_MENU, commandName, handler);
    }

    void handleInteraction(JsonNode interaction) {
        if (interaction == null) {
            return;
        }

        InteractionType interactionType = InteractionType.fromCode(interaction.path("type").asInt());
        switch (interactionType) {
            case PING -> respond(interaction, ResponseType.PONG, null);
            case APPLICATION_COMMAND -> handleApplicationCommand(interaction);
            case APPLICATION_COMMAND_AUTOCOMPLETE -> dispatchByDataField(interaction, HandlerGroup.AUTOCOMPLETE);
            case MESSAGE_COMPONENT -> dispatchByDataField(interaction, HandlerGroup.COMPONENT);
            case MODAL_SUBMIT -> dispatchByDataField(interaction, HandlerGroup.MODAL);
            case UNKNOWN -> {
                // Unknown interaction types are intentionally ignored.
            }
        }
    }

    void respondWithMessage(JsonNode interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content));
    }

    void respondWithMessage(JsonNode interaction, DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(interaction, ResponseType.CHANNEL_MESSAGE, message.toPayload());
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
        respond(interaction, ResponseType.MODAL, modal.toPayload());
    }

    void respondWithAutocompleteChoices(JsonNode interaction, List<AutocompleteChoice> choices) {
        Objects.requireNonNull(choices, "choices");

        respond(interaction, ResponseType.AUTOCOMPLETE, Map.of(
                "choices", choices.stream().map(AutocompleteChoice::toPayload).toList()
        ));
    }

    void deferMessage(JsonNode interaction) {
        respond(interaction, ResponseType.DEFERRED_CHANNEL_MESSAGE, null);
    }

    void deferUpdate(JsonNode interaction) {
        respond(interaction, ResponseType.DEFERRED_MESSAGE_UPDATE, null);
    }

    String getOptionString(JsonNode interaction, String optionName) {
        return InteractionContext.from(interaction, responder).optionString(optionName);
    }

    String getModalValue(JsonNode interaction, String customId) {
        return InteractionContext.from(interaction, responder).modalValue(customId);
    }

    private void handleApplicationCommand(JsonNode interaction) {
        CommandType commandType = CommandType.fromCode(
                interaction.path(DATA_FIELD).path("type").asInt(CommandType.CHAT_INPUT.code())
        );

        HandlerGroup handlerGroup = switch (commandType) {
            case USER_CONTEXT -> HandlerGroup.USER_CONTEXT_MENU;
            case MESSAGE_CONTEXT -> HandlerGroup.MESSAGE_CONTEXT_MENU;
            case CHAT_INPUT, UNKNOWN -> HandlerGroup.SLASH_COMMAND;
        };

        dispatchByDataField(interaction, handlerGroup);
    }

    private void dispatchByDataField(JsonNode interaction, HandlerGroup handlerGroup) {
        String handlerKey = interaction.path(DATA_FIELD).path(handlerGroup.dataField().value()).asText("").trim();
        Consumer<InteractionContext> contextHandler = handlers(handlerGroup).get(handlerKey);
        if (contextHandler == null) {
            return;
        }
        contextHandler.accept(InteractionContext.from(interaction, responder));
    }

    private void respond(JsonNode interaction, ResponseType responseType, Map<String, Object> data) {
        Objects.requireNonNull(interaction, "interaction");

        String interactionId = interaction.path(ID_FIELD).asText();
        String interactionToken = interaction.path(TOKEN_FIELD).asText();

        if (interactionId.isBlank() || interactionToken.isBlank()) {
            throw new IllegalArgumentException("interaction must include id and token");
        }

        responder.respond(interactionId, interactionToken, responseType.code(), data);
    }

    private void registerUniqueHandler(HandlerGroup handlerGroup, String key, InteractionHandler handler) {
        Objects.requireNonNull(handler, "handler");

        String normalizedKey = validateKey(key, handlerGroup.description());
        Consumer<InteractionContext> previous = handlers(handlerGroup).putIfAbsent(normalizedKey, handler::handle);
        if (previous != null) {
            throw new IllegalArgumentException("Interaction handler already registered for "
                    + handlerGroup.description() + ": " + normalizedKey);
        }
    }

    private Map<String, Consumer<InteractionContext>> handlers(HandlerGroup group) {
        return handlerRegistry.get(group);
    }

    private static String validateKey(String key, String keyType) {
        Objects.requireNonNull(key, keyType + " key");
        String normalized = key.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(keyType + " key must not be blank");
        }
        return normalized;
    }

    private static Map<HandlerGroup, Map<String, Consumer<InteractionContext>>> initializeHandlerRegistry() {
        Map<HandlerGroup, Map<String, Consumer<InteractionContext>>> registry = new EnumMap<>(HandlerGroup.class);
        for (HandlerGroup group : HandlerGroup.values()) {
            registry.put(group, new ConcurrentHashMap<>());
        }
        return Map.copyOf(registry);
    }

    private enum HandlerGroup {
        SLASH_COMMAND("slash command", DataField.NAME),
        USER_CONTEXT_MENU("user context menu", DataField.NAME),
        MESSAGE_CONTEXT_MENU("message context menu", DataField.NAME),
        COMPONENT("component", DataField.CUSTOM_ID),
        MODAL("modal", DataField.CUSTOM_ID),
        AUTOCOMPLETE("autocomplete", DataField.NAME);

        private final String description;
        private final DataField dataField;

        HandlerGroup(String description, DataField dataField) {
            this.description = description;
            this.dataField = dataField;
        }

        String description() {
            return description;
        }

        DataField dataField() {
            return dataField;
        }
    }

    private enum InteractionType {
        PING(1),
        APPLICATION_COMMAND(2),
        MESSAGE_COMPONENT(3),
        APPLICATION_COMMAND_AUTOCOMPLETE(4),
        MODAL_SUBMIT(5),
        UNKNOWN(-1);

        private static final Map<Integer, InteractionType> BY_CODE = byCode(values(), InteractionType::code);

        private final int code;

        InteractionType(int code) {
            this.code = code;
        }

        int code() {
            return code;
        }

        private static InteractionType fromCode(int code) {
            return BY_CODE.getOrDefault(code, UNKNOWN);
        }
    }

    private enum CommandType {
        CHAT_INPUT(1),
        USER_CONTEXT(2),
        MESSAGE_CONTEXT(3),
        UNKNOWN(-1);

        private static final Map<Integer, CommandType> BY_CODE = byCode(values(), CommandType::code);

        private final int code;

        CommandType(int code) {
            this.code = code;
        }

        int code() {
            return code;
        }

        private static CommandType fromCode(int code) {
            return BY_CODE.getOrDefault(code, UNKNOWN);
        }
    }

    private enum ResponseType {
        PONG(1),
        CHANNEL_MESSAGE(4),
        DEFERRED_CHANNEL_MESSAGE(5),
        DEFERRED_MESSAGE_UPDATE(6),
        AUTOCOMPLETE(8),
        MODAL(9);

        private final int code;

        ResponseType(int code) {
            this.code = code;
        }

        int code() {
            return code;
        }
    }

    private enum DataField {
        NAME("name"),
        CUSTOM_ID("custom_id");

        private final String value;

        DataField(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }
    }

    private static <T> Map<Integer, T> byCode(T[] values, Function<T, Integer> keyMapper) {
        return Arrays.stream(values).collect(Collectors.toUnmodifiableMap(keyMapper, Function.identity()));
    }

    @FunctionalInterface
    interface InteractionResponder {
        void respond(String interactionId, String interactionToken, int type, Map<String, Object> data);
    }
}
