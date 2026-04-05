package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.gateway.DiscordGatewayClient;
import com.github.cybellereaper.gateway.DiscordGatewayClient.EventDeserializer;
import com.github.cybellereaper.http.DiscordRestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class DiscordClient implements AutoCloseable {
    private final DiscordRestClient restClient;
    private final DiscordGatewayClient gatewayClient;
    private final SlashCommandRouter slashCommandRouter;
    private final DiscordApi api;
    private final CommandOperationBacklog commandBacklog = new CommandOperationBacklog();
    private final BatchOperationExecutor batchOperationExecutor = new BatchOperationExecutor();

    private volatile String applicationId;

    private DiscordClient(DiscordRestClient restClient, DiscordGatewayClient gatewayClient) {
        this.restClient = restClient;
        this.gatewayClient = gatewayClient;
        this.slashCommandRouter = new SlashCommandRouter(restClient::createInteractionResponse);
        this.api = new DiscordApi(restClient);
        this.gatewayClient.on("INTERACTION_CREATE", slashCommandRouter::handleInteraction);
        this.gatewayClient.on("READY", ignored -> commandBacklog.flush());
    }

    public static DiscordClient create(DiscordClientConfig config) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        ObjectMapper objectMapper = new ObjectMapper();

        DiscordRestClient restClient = new DiscordRestClient(httpClient, objectMapper, config);
        DiscordGatewayClient gatewayClient = new DiscordGatewayClient(httpClient, objectMapper, config, restClient);

        return new DiscordClient(restClient, gatewayClient);
    }

    public void login() {
        gatewayClient.connect();
    }

    public void on(String eventType, Consumer<JsonNode> listener) {
        gatewayClient.on(eventType, listener);
    }

    public <T> void on(String eventType, Class<T> eventClass, Consumer<T> listener) {
        gatewayClient.on(eventType, eventClass, listener);
    }

    public <T> void on(
            String eventType,
            Class<T> eventClass,
            EventDeserializer<T> deserializer,
            Consumer<T> listener
    ) {
        gatewayClient.on(eventType, eventClass, deserializer, listener);
    }

    public boolean off(String eventType, Consumer<JsonNode> listener) {
        return gatewayClient.off(eventType, listener);
    }

    public <T> boolean off(String eventType, Class<T> eventClass, Consumer<T> listener) {
        return gatewayClient.off(eventType, eventClass, listener);
    }

    public void onSlashCommand(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerSlashHandler(commandName, listener);
    }

    public void onSlashCommandContext(String commandName, SlashCommandHandler listener) {
        slashCommandRouter.registerSlashContextHandler(commandName, listener);
    }

    public void onAutocomplete(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerAutocompleteHandler(commandName, listener);
    }

    public void onAutocompleteContext(String commandName, SlashCommandHandler listener) {
        slashCommandRouter.registerAutocompleteContextHandler(commandName, listener);
    }

    public void onUserContextMenu(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerUserContextMenuHandler(commandName, listener);
    }

    public void onUserContextMenuContext(String commandName, InteractionHandler listener) {
        slashCommandRouter.registerUserContextMenuContextHandler(commandName, listener);
    }

    public void onMessageContextMenu(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerMessageContextMenuHandler(commandName, listener);
    }

    public void onMessageContextMenuContext(String commandName, InteractionHandler listener) {
        slashCommandRouter.registerMessageContextMenuContextHandler(commandName, listener);
    }

    public void onComponentInteraction(String customId, Consumer<JsonNode> listener) {
        slashCommandRouter.registerComponentHandler(customId, listener);
    }

    public void onComponentInteractionContext(String customId, InteractionHandler listener) {
        slashCommandRouter.registerComponentContextHandler(customId, listener);
    }

    public void onComponentInteractionPrefix(String customIdPrefix, Consumer<JsonNode> listener) {
        slashCommandRouter.registerComponentPrefixHandler(customIdPrefix, listener);
    }

    public void onComponentInteractionContextPrefix(String customIdPrefix, InteractionHandler listener) {
        slashCommandRouter.registerComponentPrefixContextHandler(customIdPrefix, listener);
    }

    public void onModalSubmit(String customId, Consumer<JsonNode> listener) {
        slashCommandRouter.registerModalHandler(customId, listener);
    }

    public void onModalSubmitContext(String customId, ModalSubmitHandler listener) {
        slashCommandRouter.registerModalContextHandler(customId, listener);
    }

    public void onModalSubmitPrefix(String customIdPrefix, Consumer<JsonNode> listener) {
        slashCommandRouter.registerModalPrefixHandler(customIdPrefix, listener);
    }

    public void onModalSubmitContextPrefix(String customIdPrefix, ModalSubmitHandler listener) {
        slashCommandRouter.registerModalPrefixContextHandler(customIdPrefix, listener);
    }

    public DiscordApi api() {
        return api;
    }

    public JsonNode registerGlobalSlashCommand(String commandName, String description) {
        return registerGlobalSlashCommand(SlashCommandDefinition.simple(commandName, description));
    }

    public JsonNode registerGlobalSlashCommand(SlashCommandDefinition command) {
        Objects.requireNonNull(command, "command");
        String operationKey = "global:create:" + commandFingerprint(command);
        return commandBacklog.execute(
                operationKey,
                () -> restClient.createGlobalApplicationCommand(resolveApplicationId(), command)
        );
    }

    public JsonNode registerGlobalUserContextMenu(String commandName) {
        return registerGlobalSlashCommand(SlashCommandDefinition.userContextMenu(commandName));
    }

    public JsonNode registerGlobalMessageContextMenu(String commandName) {
        return registerGlobalSlashCommand(SlashCommandDefinition.messageContextMenu(commandName));
    }

    public void registerGlobalSlashCommands(List<SlashCommandDefinition> commands) {
        registerCommands(commands, this::registerGlobalSlashCommand);
    }

    public void syncGlobalSlashCommands(List<SlashCommandDefinition> commands) {
        Objects.requireNonNull(commands, "commands");
        commandBacklog.execute(
                "global:sync",
                () -> restClient.bulkOverwriteGlobalApplicationCommands(resolveApplicationId(), commands)
        );
    }

    public JsonNode registerGuildSlashCommand(String guildId, String commandName, String description) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.simple(commandName, description));
    }

    public JsonNode registerGuildSlashCommand(String guildId, SlashCommandDefinition command) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(command, "command");

        String operationKey = "guild:create:" + guildId + ":" + commandFingerprint(command);
        return commandBacklog.execute(
                operationKey,
                () -> restClient.createGuildApplicationCommand(resolveApplicationId(), guildId, command)
        );
    }

    public JsonNode registerGuildUserContextMenu(String guildId, String commandName) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.userContextMenu(commandName));
    }

    public JsonNode registerGuildMessageContextMenu(String guildId, String commandName) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.messageContextMenu(commandName));
    }

    public void registerGuildSlashCommands(String guildId, List<SlashCommandDefinition> commands) {
        requireNonBlank(guildId, "guildId");
        registerCommands(commands, command -> registerGuildSlashCommand(guildId, command));
    }

    public void syncGuildSlashCommands(String guildId, List<SlashCommandDefinition> commands) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(commands, "commands");
        String operationKey = "guild:sync:" + guildId;
        commandBacklog.execute(
                operationKey,
                () -> restClient.bulkOverwriteGuildApplicationCommands(resolveApplicationId(), guildId, commands)
        );
    }

    public void respondWithMessage(JsonNode interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content));
    }

    public void respondWithMessage(InteractionContext interaction, String content) {
        Objects.requireNonNull(interaction, "interaction");
        respondWithMessage(interaction.raw(), content);
    }

    public void respondWithMessage(JsonNode interaction, DiscordMessage message) {
        slashCommandRouter.respondWithMessage(interaction, message);
    }

    public void respondWithMessage(InteractionContext interaction, DiscordMessage message) {
        Objects.requireNonNull(interaction, "interaction");
        respondWithMessage(interaction.raw(), message);
    }

    public void respondWithEmbeds(JsonNode interaction, String content, List<DiscordEmbed> embeds) {
        slashCommandRouter.respondWithEmbeds(interaction, content, embeds);
    }

    public void respondWithEmbeds(InteractionContext interaction, String content, List<DiscordEmbed> embeds) {
        Objects.requireNonNull(interaction, "interaction");
        respondWithEmbeds(interaction.raw(), content, embeds);
    }

    public void respondEphemeral(JsonNode interaction, String content) {
        slashCommandRouter.respondEphemeral(interaction, content);
    }

    public void respondEphemeral(InteractionContext interaction, String content) {
        Objects.requireNonNull(interaction, "interaction");
        respondEphemeral(interaction.raw(), content);
    }

    public void respondEphemeralWithEmbeds(JsonNode interaction, String content, List<DiscordEmbed> embeds) {
        slashCommandRouter.respondEphemeralWithEmbeds(interaction, content, embeds);
    }

    public void respondEphemeralWithEmbeds(InteractionContext interaction, String content, List<DiscordEmbed> embeds) {
        Objects.requireNonNull(interaction, "interaction");
        respondEphemeralWithEmbeds(interaction.raw(), content, embeds);
    }

    public void respondWithModal(JsonNode interaction, DiscordModal modal) {
        slashCommandRouter.respondWithModal(interaction, modal);
    }

    public void respondWithModal(InteractionContext interaction, DiscordModal modal) {
        Objects.requireNonNull(interaction, "interaction");
        respondWithModal(interaction.raw(), modal);
    }

    public void respondWithAutocompleteChoices(JsonNode interaction, List<AutocompleteChoice> choices) {
        slashCommandRouter.respondWithAutocompleteChoices(interaction, choices);
    }

    public void respondWithAutocompleteChoices(InteractionContext interaction, List<AutocompleteChoice> choices) {
        Objects.requireNonNull(interaction, "interaction");
        respondWithAutocompleteChoices(interaction.raw(), choices);
    }

    public void deferMessage(JsonNode interaction) {
        slashCommandRouter.deferMessage(interaction);
    }

    public void deferMessage(InteractionContext interaction) {
        Objects.requireNonNull(interaction, "interaction");
        deferMessage(interaction.raw());
    }

    public void deferUpdate(JsonNode interaction) {
        slashCommandRouter.deferUpdate(interaction);
    }

    public void deferUpdate(InteractionContext interaction) {
        Objects.requireNonNull(interaction, "interaction");
        deferUpdate(interaction.raw());
    }

    public String getStringOption(JsonNode interaction, String optionName) {
        return slashCommandRouter.getOptionString(interaction, optionName);
    }

    public String getStringOption(SlashCommandInteraction interaction, String optionName) {
        Objects.requireNonNull(interaction, "interaction");
        return interaction.parameters().getString(optionName);
    }

    public String getModalValue(JsonNode interaction, String customId) {
        return slashCommandRouter.getModalValue(interaction, customId);
    }

    public String getModalValue(ModalSubmitInteraction interaction, String customId) {
        Objects.requireNonNull(interaction, "interaction");
        return interaction.parameters().getString(customId);
    }

    public void sendMessage(String channelId, String content) {
        sendMessage(channelId, DiscordMessage.ofContent(content));
    }

    public void sendMessage(String channelId, DiscordMessage message) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        restClient.sendMessage(channelId, message.toPayload());
    }

    public void sendMessageWithEmbeds(String channelId, String content, List<DiscordEmbed> embeds) {
        sendMessage(channelId, DiscordMessage.ofEmbeds(content, embeds));
    }

    @Override
    public void close() {
        gatewayClient.close();
    }

    private void registerCommands(List<SlashCommandDefinition> commands, Consumer<SlashCommandDefinition> registrar) {
        batchOperationExecutor.executeAll(commands, registrar);
    }

    private void deleteExistingGlobalCommands(String applicationId) {
        for (JsonNode existingCommand : restClient.getGlobalApplicationCommands(applicationId)) {
            String commandId = requireCommandId(existingCommand);
            restClient.deleteGlobalApplicationCommand(applicationId, commandId);
        }
    }

    private void deleteExistingGuildCommands(String applicationId, String guildId) {
        for (JsonNode existingCommand : restClient.getGuildApplicationCommands(applicationId, guildId)) {
            String commandId = requireCommandId(existingCommand);
            restClient.deleteGuildApplicationCommand(applicationId, guildId, commandId);
        }
    }


    private static String commandFingerprint(SlashCommandDefinition command) {
        return Integer.toHexString(command.toRequestPayload().hashCode());
    }

    private static String requireCommandId(JsonNode command) {
        Objects.requireNonNull(command, "command");
        String commandId = command.path("id").asText("");
        if (commandId.isBlank()) {
            throw new IllegalStateException("Discord command payload is missing id");
        }
        return commandId;
    }

    private String resolveApplicationId() {
        String current = applicationId;
        if (current != null && !current.isBlank()) {
            return current;
        }

        synchronized (this) {
            if (applicationId == null || applicationId.isBlank()) {
                applicationId = restClient.getCurrentApplicationId();
            }
            return applicationId;
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
