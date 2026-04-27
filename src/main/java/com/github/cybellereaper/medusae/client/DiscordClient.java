package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;
import com.github.cybellereaper.medusae.gateway.DiscordGatewayClient;
import com.github.cybellereaper.medusae.gateway.DiscordGatewayClient.EventDeserializer;
import com.github.cybellereaper.medusae.http.DiscordApplication;
import com.github.cybellereaper.medusae.http.DiscordRestClient;
import com.github.cybellereaper.medusae.http.RateLimitObserver;
import com.github.cybellereaper.medusae.http.RetryPolicy;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
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
        this.api = new DiscordApi(restClient, stateCache);
        this.stateCache = stateCache;
        this.gatewayClient.on("INTERACTION_CREATE", DiscordInteractionPayload.class, slashCommandRouter::handleInteraction);
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

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public void login() {
        gatewayClient.connect();
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

    public <T> boolean off(String eventType, Class<T> eventClass, Consumer<T> listener) {
        return gatewayClient.off(eventType, eventClass, listener);
    }

    public void onSlashCommand(String commandName, Consumer<DiscordInteractionPayload> listener) {
        slashCommandRouter.registerSlashHandler(commandName, listener);
    }

    public void onSlashCommandContext(String commandName, InteractionHandler listener) {
        slashCommandRouter.registerSlashContextHandler(commandName, listener);
    }

    public void onAutocomplete(String commandName, Consumer<DiscordInteractionPayload> listener) {
        slashCommandRouter.registerAutocompleteHandler(commandName, listener);
    }

    public void onAutocompleteContext(String commandName, InteractionHandler listener) {
        slashCommandRouter.registerAutocompleteContextHandler(commandName, listener);
    }

    public void onUserContextMenu(String commandName, Consumer<DiscordInteractionPayload> listener) {
        slashCommandRouter.registerUserContextMenuHandler(commandName, listener);
    }

    public void onUserContextMenuContext(String commandName, InteractionHandler listener) {
        slashCommandRouter.registerUserContextMenuContextHandler(commandName, listener);
    }

    public void onMessageContextMenu(String commandName, Consumer<DiscordInteractionPayload> listener) {
        slashCommandRouter.registerMessageContextMenuHandler(commandName, listener);
    }

    public void onMessageContextMenuContext(String commandName, InteractionHandler listener) {
        slashCommandRouter.registerMessageContextMenuContextHandler(commandName, listener);
    }

    public void onComponentInteraction(String customId, Consumer<DiscordInteractionPayload> listener) {
        slashCommandRouter.registerComponentHandler(customId, listener);
    }

    public void onComponentInteractionContext(String customId, InteractionHandler listener) {
        slashCommandRouter.registerComponentContextHandler(customId, listener);
    }

    public void onAnyComponentInteractionContext(InteractionHandler listener) {
        slashCommandRouter.registerGlobalComponentContextHandler(listener);
    }

    public void onModalSubmit(String customId, Consumer<DiscordInteractionPayload> listener) {
        slashCommandRouter.registerModalHandler(customId, listener);
    }

    public void onModalSubmitContext(String customId, InteractionHandler listener) {
        slashCommandRouter.registerModalContextHandler(customId, listener);
    }

    public void onAnyModalSubmitContext(InteractionHandler listener) {
        slashCommandRouter.registerGlobalModalContextHandler(listener);
    }

    public DiscordApi api() {
        return api;
    }

    public Optional<DiscordStateCache> stateCache() {
        return Optional.ofNullable(stateCache);
    }

    public Map<String, Object> registerGlobalSlashCommand(String commandName, String description) {
        return registerGlobalSlashCommand(SlashCommandDefinition.simple(commandName, description));
    }

    public Map<String, Object> registerGlobalSlashCommand(SlashCommandDefinition command) {
        Objects.requireNonNull(command, "command");
        return restClient.createGlobalApplicationCommand(resolveApplicationId(), command);
    }

    public Map<String, Object> registerGlobalUserContextMenu(String commandName) {
        return registerGlobalSlashCommand(SlashCommandDefinition.userContextMenu(commandName));
    }

    public Map<String, Object> registerGlobalMessageContextMenu(String commandName) {
        return registerGlobalSlashCommand(SlashCommandDefinition.messageContextMenu(commandName));
    }

    public void registerGlobalSlashCommands(List<SlashCommandDefinition> commands) {
        registerCommands(commands, this::registerGlobalSlashCommand);
    }

    public Map<String, Object> registerGuildSlashCommand(String guildId, String commandName, String description) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.simple(commandName, description));
    }

    public Map<String, Object> registerGuildSlashCommand(String guildId, SlashCommandDefinition command) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(command, "command");

        return restClient.createGuildApplicationCommand(resolveApplicationId(), guildId, command);
    }

    public Map<String, Object> registerGuildUserContextMenu(String guildId, String commandName) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.userContextMenu(commandName));
    }

    public Map<String, Object> registerGuildMessageContextMenu(String guildId, String commandName) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.messageContextMenu(commandName));
    }

    public void registerGuildSlashCommands(String guildId, List<SlashCommandDefinition> commands) {
        requireNonBlank(guildId, "guildId");
        registerCommands(commands, command -> registerGuildSlashCommand(guildId, command));
    }

    public void respondWithMessage(DiscordInteractionPayload interaction, String content) {
        respondWithMessage(interaction, DiscordMessage.ofContent(content));
    }

    public void respondWithMessage(DiscordInteractionPayload interaction, DiscordMessage message) {
        slashCommandRouter.respondWithMessage(interaction, message);
    }

    public void respondWithUpdatedMessage(DiscordInteractionPayload interaction, DiscordMessage message) {
        slashCommandRouter.respondWithUpdatedMessage(interaction, message);
    }

    public void respondWithEmbeds(DiscordInteractionPayload interaction, String content, List<DiscordEmbed> embeds) {
        slashCommandRouter.respondWithEmbeds(interaction, content, embeds);
    }

    public void respondEphemeral(DiscordInteractionPayload interaction, String content) {
        slashCommandRouter.respondEphemeral(interaction, content);
    }

    public void respondEphemeralWithEmbeds(DiscordInteractionPayload interaction, String content, List<DiscordEmbed> embeds) {
        slashCommandRouter.respondEphemeralWithEmbeds(interaction, content, embeds);
    }

    public void respondWithModal(DiscordInteractionPayload interaction, DiscordModal modal) {
        slashCommandRouter.respondWithModal(interaction, modal);
    }

    public void respondWithAutocompleteChoices(DiscordInteractionPayload interaction, List<AutocompleteChoice> choices) {
        slashCommandRouter.respondWithAutocompleteChoices(interaction, choices);
    }

    public void deferMessage(DiscordInteractionPayload interaction) {
        slashCommandRouter.deferMessage(interaction);
    }

    public void deferUpdate(DiscordInteractionPayload interaction) {
        slashCommandRouter.deferUpdate(interaction);
    }

    public String getStringOption(DiscordInteractionPayload interaction, String optionName) {
        return slashCommandRouter.getOptionString(interaction, optionName);
    }

    public String getModalValue(DiscordInteractionPayload interaction, String customId) {
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

        gatewayClient.on("GUILD_CREATE", GuildSnapshot.class, stateCache::putGuild);
        gatewayClient.on("GUILD_UPDATE", GuildSnapshot.class, stateCache::putGuild);
        gatewayClient.on("GUILD_DELETE", GuildSnapshot.class, payload -> stateCache.removeGuild(stringOrEmpty(payload.id())));

        gatewayClient.on("CHANNEL_CREATE", ChannelSnapshot.class, stateCache::putChannel);
        gatewayClient.on("CHANNEL_UPDATE", ChannelSnapshot.class, stateCache::putChannel);
        gatewayClient.on("CHANNEL_DELETE", ChannelSnapshot.class, payload -> stateCache.removeChannel(stringOrEmpty(payload.id())));

        gatewayClient.on("GUILD_MEMBER_ADD", GuildMemberSnapshot.class, stateCache::putMember);
        gatewayClient.on("GUILD_MEMBER_UPDATE", GuildMemberSnapshot.class, stateCache::putMember);
        gatewayClient.on("GUILD_MEMBER_REMOVE", GuildMemberSnapshot.class, payload -> {
            String guildId = stringOrEmpty(payload.guildId());
            String userId = payload.user() == null ? "" : stringOrEmpty(payload.user().id());
            stateCache.removeMember(guildId, userId);
        });

        gatewayClient.on("GUILD_ROLE_CREATE", GuildScopedEvent.class, payload -> stateCache.invalidateGuildRoles(stringOrEmpty(payload.guildId())));
        gatewayClient.on("GUILD_ROLE_UPDATE", GuildScopedEvent.class, payload -> stateCache.invalidateGuildRoles(stringOrEmpty(payload.guildId())));
        gatewayClient.on("GUILD_ROLE_DELETE", GuildScopedEvent.class, payload -> stateCache.invalidateGuildRoles(stringOrEmpty(payload.guildId())));

        gatewayClient.on("GUILD_EMOJIS_UPDATE", GuildScopedEvent.class, payload -> stateCache.invalidateGuildEmojis(stringOrEmpty(payload.guildId())));
        gatewayClient.on("WEBHOOKS_UPDATE", WebhooksUpdateEvent.class, payload -> {
            String guildId = stringOrEmpty(payload.guildId());
            String channelId = stringOrEmpty(payload.channelId());
            stateCache.invalidateGuildWebhooks(guildId);
            stateCache.invalidateChannelWebhooks(channelId);
        });
        gatewayClient.on("GUILD_SCHEDULED_EVENT_CREATE", GuildScopedEvent.class, payload -> stateCache.invalidateScheduledEvents(stringOrEmpty(payload.guildId())));
        gatewayClient.on("GUILD_SCHEDULED_EVENT_UPDATE", GuildScopedEvent.class, payload -> stateCache.invalidateScheduledEvents(stringOrEmpty(payload.guildId())));
        gatewayClient.on("GUILD_SCHEDULED_EVENT_DELETE", GuildScopedEvent.class, payload -> stateCache.invalidateScheduledEvents(stringOrEmpty(payload.guildId())));
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

    private static String stringOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
