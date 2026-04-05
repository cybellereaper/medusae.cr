package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.gateway.DiscordGatewayClient;
import com.github.cybellereaper.gateway.DiscordGatewayClient.EventDeserializer;
import com.github.cybellereaper.http.DiscordRestClient;
import com.github.cybellereaper.http.RateLimitObserver;
import com.github.cybellereaper.http.RetryPolicy;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class DiscordClient implements AutoCloseable {
    private final DiscordRestClient restClient;
    private final DiscordGatewayClient gatewayClient;
    private final SlashCommandRouter slashCommandRouter;
    private final DiscordApi api;
    private final DiscordStateCache stateCache;

    private volatile String applicationId;

    private DiscordClient(DiscordRestClient restClient, DiscordGatewayClient gatewayClient, DiscordStateCache stateCache) {
        this.restClient = restClient;
        this.gatewayClient = gatewayClient;
        this.slashCommandRouter = new SlashCommandRouter(restClient::createInteractionResponse);
        this.api = new DiscordApi(restClient);
        this.stateCache = stateCache;
        this.gatewayClient.on("INTERACTION_CREATE", slashCommandRouter::handleInteraction);
        registerCacheListeners();
    }

    public static DiscordClient create(DiscordClientConfig config) {
        return create(config, RetryPolicy.defaultPolicy(), RateLimitObserver.NOOP, false);
    }

    public static DiscordClient create(
            DiscordClientConfig config,
            RetryPolicy retryPolicy,
            RateLimitObserver rateLimitObserver,
            boolean enableStateCache
    ) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        ObjectMapper objectMapper = new ObjectMapper();

        DiscordRestClient restClient = new DiscordRestClient(httpClient, objectMapper, config, retryPolicy, rateLimitObserver);
        DiscordGatewayClient gatewayClient = new DiscordGatewayClient(httpClient, objectMapper, config, restClient);

        DiscordStateCache stateCache = enableStateCache ? new DiscordStateCache() : null;
        return new DiscordClient(restClient, gatewayClient, stateCache);
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

    public void onAutocomplete(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerAutocompleteHandler(commandName, listener);
    }

    public void onUserContextMenu(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerUserContextMenuHandler(commandName, listener);
    }

    public void onMessageContextMenu(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerMessageContextMenuHandler(commandName, listener);
    }

    public void onComponentInteraction(String customId, Consumer<JsonNode> listener) {
        slashCommandRouter.registerComponentHandler(customId, listener);
    }

    public void onModalSubmit(String customId, Consumer<JsonNode> listener) {
        slashCommandRouter.registerModalHandler(customId, listener);
    }

    public DiscordApi api() {
        return api;
    }

    public Optional<DiscordStateCache> stateCache() {
        return Optional.ofNullable(stateCache);
    }

    public JsonNode registerGlobalSlashCommand(String commandName, String description) {
        return registerGlobalSlashCommand(SlashCommandDefinition.simple(commandName, description));
    }

    public JsonNode registerGlobalSlashCommand(SlashCommandDefinition command) {
        Objects.requireNonNull(command, "command");
        return restClient.createGlobalApplicationCommand(resolveApplicationId(), command);
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

    public JsonNode registerGuildSlashCommand(String guildId, String commandName, String description) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.simple(commandName, description));
    }

    public JsonNode registerGuildSlashCommand(String guildId, SlashCommandDefinition command) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(command, "command");

        return restClient.createGuildApplicationCommand(resolveApplicationId(), guildId, command);
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

    public void respondWithMessage(JsonNode interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content));
    }

    public void respondWithMessage(JsonNode interaction, DiscordMessage message) {
        slashCommandRouter.respondWithMessage(interaction, message);
    }

    public void respondWithEmbeds(JsonNode interaction, String content, List<DiscordEmbed> embeds) {
        slashCommandRouter.respondWithEmbeds(interaction, content, embeds);
    }

    public void respondEphemeral(JsonNode interaction, String content) {
        slashCommandRouter.respondEphemeral(interaction, content);
    }

    public void respondEphemeralWithEmbeds(JsonNode interaction, String content, List<DiscordEmbed> embeds) {
        slashCommandRouter.respondEphemeralWithEmbeds(interaction, content, embeds);
    }

    public void respondWithModal(JsonNode interaction, DiscordModal modal) {
        slashCommandRouter.respondWithModal(interaction, modal);
    }

    public void respondWithAutocompleteChoices(JsonNode interaction, List<AutocompleteChoice> choices) {
        slashCommandRouter.respondWithAutocompleteChoices(interaction, choices);
    }

    public void deferMessage(JsonNode interaction) {
        slashCommandRouter.deferMessage(interaction);
    }

    public void deferUpdate(JsonNode interaction) {
        slashCommandRouter.deferUpdate(interaction);
    }

    public String getStringOption(JsonNode interaction, String optionName) {
        return slashCommandRouter.getOptionString(interaction, optionName);
    }

    public String getModalValue(JsonNode interaction, String customId) {
        return slashCommandRouter.getModalValue(interaction, customId);
    }

    public void sendMessage(String channelId, String content) {
        sendMessage(channelId, DiscordMessage.ofContent(content));
    }

    public void sendMessage(String channelId, DiscordMessage message) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        restClient.sendMessage(channelId, message.toPayload());
    }

    public void sendMessageWithAttachments(String channelId, DiscordMessage message, List<DiscordAttachment> attachments) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(attachments, "attachments");
        restClient.sendMessageWithAttachments(channelId, message.toPayload(), attachments);
    }

    public void sendMessageWithEmbeds(String channelId, String content, List<DiscordEmbed> embeds) {
        sendMessage(channelId, DiscordMessage.ofEmbeds(content, embeds));
    }

    @Override
    public void close() {
        gatewayClient.close();
    }

    private void registerCommands(List<SlashCommandDefinition> commands, Consumer<SlashCommandDefinition> registrar) {
        Objects.requireNonNull(commands, "commands");
        for (SlashCommandDefinition command : commands) {
            registrar.accept(command);
        }
    }

    private void registerCacheListeners() {
        if (stateCache == null) {
            return;
        }

        gatewayClient.on("GUILD_CREATE", stateCache::putGuild);
        gatewayClient.on("CHANNEL_CREATE", stateCache::putChannel);
        gatewayClient.on("CHANNEL_UPDATE", stateCache::putChannel);
        gatewayClient.on("GUILD_MEMBER_ADD", stateCache::putMember);
        gatewayClient.on("GUILD_MEMBER_UPDATE", stateCache::putMember);
        gatewayClient.on("GUILD_MEMBER_REMOVE", payload -> {
            String guildId = payload.path("guild_id").asText("");
            String userId = payload.path("user").path("id").asText("");
            stateCache.removeMember(guildId, userId);
        });
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
