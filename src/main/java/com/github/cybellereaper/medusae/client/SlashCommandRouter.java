package com.github.cybellereaper.medusae.client;

import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

final class SlashCommandRouter {

    private final Map<String, Consumer<InteractionContext>> slashHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<InteractionContext>> userContextMenuHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<InteractionContext>> messageContextMenuHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<InteractionContext>> componentHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<InteractionContext>> modalHandlers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<InteractionContext>> autocompleteHandlers = new ConcurrentHashMap<>();
    private final List<Consumer<InteractionContext>> globalComponentHandlers = new CopyOnWriteArrayList<>();
    private final List<Consumer<InteractionContext>> globalModalHandlers = new CopyOnWriteArrayList<>();
    private final InteractionResponder responder;

    SlashCommandRouter(InteractionResponder responder) {
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    private static void registerUniqueHandler(
            Map<String, Consumer<InteractionContext>> handlers,
            String key,
            String handlerType,
            Consumer<InteractionContext> handler
    ) {
        String normalizedKey = validateKey(key, handlerType);
        Objects.requireNonNull(handler, "handler");

        Consumer<InteractionContext> previous = handlers.putIfAbsent(normalizedKey, handler);
        if (previous != null) {
            throw new IllegalArgumentException("Interaction handler already registered for " + handlerType + ": " + normalizedKey);
        }
    }

    private static String validateKey(String key, String keyType) {
        Objects.requireNonNull(key, keyType + " key");
        String normalized = key.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(keyType + " key must not be blank");
        }
        return normalized;
    }

    private static <T> Map<Integer, T> byCode(T[] values, Function<T, Integer> keyMapper) {
        return Arrays.stream(values).collect(Collectors.toUnmodifiableMap(keyMapper, Function.identity()));
    }

    void registerSlashHandler(String commandName, Consumer<DiscordInteractionPayload> handler) {
        Objects.requireNonNull(handler, "handler");
        registerSlashContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerComponentHandler(String customId, Consumer<DiscordInteractionPayload> handler) {
        Objects.requireNonNull(handler, "handler");
        registerComponentContextHandler(customId, context -> handler.accept(context.raw()));
    }

    void registerModalHandler(String customId, Consumer<DiscordInteractionPayload> handler) {
        Objects.requireNonNull(handler, "handler");
        registerModalContextHandler(customId, context -> handler.accept(context.raw()));
    }

    void registerAutocompleteHandler(String commandName, Consumer<DiscordInteractionPayload> handler) {
        Objects.requireNonNull(handler, "handler");
        registerAutocompleteContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerUserContextMenuHandler(String commandName, Consumer<DiscordInteractionPayload> handler) {
        Objects.requireNonNull(handler, "handler");
        registerUserContextMenuContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerMessageContextMenuHandler(String commandName, Consumer<DiscordInteractionPayload> handler) {
        Objects.requireNonNull(handler, "handler");
        registerMessageContextMenuContextHandler(commandName, context -> handler.accept(context.raw()));
    }

    void registerSlashContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(slashHandlers, commandName, "slash command", handler::handle);
    }

    void registerComponentContextHandler(String customId, InteractionHandler handler) {
        registerUniqueHandler(componentHandlers, customId, "component", handler::handle);
    }

    void registerModalContextHandler(String customId, InteractionHandler handler) {
        registerUniqueHandler(modalHandlers, customId, "modal", handler::handle);
    }

    void registerGlobalComponentContextHandler(InteractionHandler handler) {
        Objects.requireNonNull(handler, "handler");
        globalComponentHandlers.add(handler::handle);
    }

    void registerGlobalModalContextHandler(InteractionHandler handler) {
        Objects.requireNonNull(handler, "handler");
        globalModalHandlers.add(handler::handle);
    }

    void registerAutocompleteContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(autocompleteHandlers, commandName, "autocomplete", handler::handle);
    }

    void registerUserContextMenuContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(userContextMenuHandlers, commandName, "user context menu", handler::handle);
    }

    void registerMessageContextMenuContextHandler(String commandName, InteractionHandler handler) {
        registerUniqueHandler(messageContextMenuHandlers, commandName, "message context menu", handler::handle);
    }

    void handleInteraction(DiscordInteractionPayload interaction) {
        if (interaction == null) {
            return;
        }

        InteractionType interactionType = InteractionType.fromCode(interaction.typeOrZero());
        switch (interactionType) {
            case PING -> respond(interaction, ResponseType.PONG, null);
            case APPLICATION_COMMAND -> handleApplicationCommand(interaction);
            case APPLICATION_COMMAND_AUTOCOMPLETE ->
                    dispatchByDataField(interaction, autocompleteHandlers, DataField.NAME);
            case MESSAGE_COMPONENT ->
                    dispatchByDataField(interaction, componentHandlers, globalComponentHandlers, DataField.CUSTOM_ID);
            case MODAL_SUBMIT ->
                    dispatchByDataField(interaction, modalHandlers, globalModalHandlers, DataField.CUSTOM_ID);
            case UNKNOWN -> {
                // Unknown interaction types are intentionally ignored.
            }
        }
    }

    void respondWithMessage(DiscordInteractionPayload interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content));
    }

    void respondWithMessage(DiscordInteractionPayload interaction, DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(interaction, ResponseType.CHANNEL_MESSAGE, message.toPayload());
    }

    void respondWithUpdatedMessage(DiscordInteractionPayload interaction, DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(interaction, ResponseType.UPDATE_MESSAGE, message.toPayload());
    }

    void respondWithEmbeds(DiscordInteractionPayload interaction, String content, List<DiscordEmbed> embeds) {
        respondWithMessage(interaction, DiscordMessage.ofEmbeds(content, embeds));
    }

    void respondEphemeral(DiscordInteractionPayload interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content).asEphemeral());
    }

    void respondEphemeralWithEmbeds(DiscordInteractionPayload interaction, String content, List<DiscordEmbed> embeds) {
        respondWithMessage(interaction, DiscordMessage.ofEmbeds(content, embeds).asEphemeral());
    }

    void respondWithModal(DiscordInteractionPayload interaction, DiscordModal modal) {
        Objects.requireNonNull(modal, "modal");
        respond(interaction, ResponseType.MODAL, modal.toPayload());
    }

    void respondWithAutocompleteChoices(DiscordInteractionPayload interaction, List<AutocompleteChoice> choices) {
        Objects.requireNonNull(choices, "choices");

        respond(interaction, ResponseType.AUTOCOMPLETE, Map.of(
                "choices", choices.stream().map(AutocompleteChoice::toPayload).toList()
        ));
    }

    void deferMessage(DiscordInteractionPayload interaction) {
        respond(interaction, ResponseType.DEFERRED_CHANNEL_MESSAGE, null);
    }

    void deferUpdate(DiscordInteractionPayload interaction) {
        respond(interaction, ResponseType.DEFERRED_MESSAGE_UPDATE, null);
    }

    String getOptionString(DiscordInteractionPayload interaction, String optionName) {
        return InteractionContext.from(interaction, responder).optionString(optionName);
    }

    String getModalValue(DiscordInteractionPayload interaction, String customId) {
        return InteractionContext.from(interaction, responder).modalValue(customId);
    }

    private void handleApplicationCommand(DiscordInteractionPayload interaction) {
        CommandType commandType = CommandType.fromCode(
                interaction.data() == null || interaction.data().type() == null
                        ? CommandType.CHAT_INPUT.code()
                        : interaction.data().type()
        );

        Map<String, Consumer<InteractionContext>> handlers = switch (commandType) {
            case USER_CONTEXT -> userContextMenuHandlers;
            case MESSAGE_CONTEXT -> messageContextMenuHandlers;
            case CHAT_INPUT, UNKNOWN -> slashHandlers;
        };

        dispatchByDataField(interaction, handlers, DataField.NAME);
    }

    private void dispatchByDataField(
            DiscordInteractionPayload interaction,
            Map<String, Consumer<InteractionContext>> handlers,
            List<Consumer<InteractionContext>> globalHandlers,
            DataField dataField
    ) {
        String handlerKey = switch (dataField) {
            case NAME -> interaction.data() == null ? "" : stringOrEmpty(interaction.data().name()).trim();
            case CUSTOM_ID -> interaction.data() == null ? "" : stringOrEmpty(interaction.data().customId()).trim();
        };
        InteractionContext context = InteractionContext.from(interaction, responder);
        Consumer<InteractionContext> contextHandler = handlers.get(handlerKey);
        if (contextHandler != null) {
            contextHandler.accept(context);
            return;
        }

        for (Consumer<InteractionContext> globalHandler : globalHandlers) {
            globalHandler.accept(context);
        }
    }

    private void dispatchByDataField(DiscordInteractionPayload interaction, Map<String, Consumer<InteractionContext>> handlers, DataField dataField) {
        dispatchByDataField(interaction, handlers, List.of(), dataField);
    }

    private void respond(DiscordInteractionPayload interaction, ResponseType responseType, Map<String, Object> data) {
        Objects.requireNonNull(interaction, "interaction");

        String interactionId = stringOrEmpty(interaction.id());
        String interactionToken = stringOrEmpty(interaction.token());

        if (interactionId.isBlank() || interactionToken.isBlank()) {
            throw new IllegalArgumentException("interaction must include id and token");
        }

        responder.respond(interactionId, interactionToken, responseType.code(), data);
    }

    private static String stringOrEmpty(String value) {
        return value == null ? "" : value;
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

        private static InteractionType fromCode(int code) {
            return BY_CODE.getOrDefault(code, UNKNOWN);
        }

        int code() {
            return code;
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

        private static CommandType fromCode(int code) {
            return BY_CODE.getOrDefault(code, UNKNOWN);
        }

        int code() {
            return code;
        }
    }

    private enum ResponseType {
        PONG(1),
        CHANNEL_MESSAGE(4),
        DEFERRED_CHANNEL_MESSAGE(5),
        DEFERRED_MESSAGE_UPDATE(6),
        UPDATE_MESSAGE(7),
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

    @FunctionalInterface
    interface InteractionResponder {
        void respond(String interactionId, String interactionToken, int type, Map<String, Object> data);
    }
}
