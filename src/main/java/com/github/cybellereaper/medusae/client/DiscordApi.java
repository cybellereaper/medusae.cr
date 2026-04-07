package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.medusae.http.DiscordRestClient;

import java.util.*;
import java.util.function.Supplier;

/**
 * High-level REST API helper for common Discord resources.
 */
public final class DiscordApi {
    private final DiscordRestClient restClient;
    private final DiscordStateCache stateCache;

    DiscordApi(DiscordRestClient restClient) {
        this(restClient, null);
    }

    DiscordApi(DiscordRestClient restClient, DiscordStateCache stateCache) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.stateCache = stateCache;
    }

    private static Optional<JsonNode> cache(Supplier<Optional<JsonNode>> accessor) {
        return accessor.get().map(JsonNode::deepCopy);
    }

    private static JsonNode getOrLoad(Optional<JsonNode> cached, Supplier<JsonNode> loader) {
        return cached.orElseGet(loader);
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public JsonNode getCurrentApplication() {
        return restClient.getCurrentApplication();
    }

    public JsonNode getCurrentUser() {
        return restClient.request("GET", "/users/@me", null);
    }

    public JsonNode getChannel(String channelId) {
        requireNonBlank(channelId, "channelId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getChannel(channelId)),
                () -> {
                    JsonNode channel = restClient.request("GET", "/channels/" + channelId, null);
                    if (stateCache != null) {
                        stateCache.putChannel(channel);
                    }
                    return channel;
                }
        );
    }

    public JsonNode getGuild(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getGuild(guildId)),
                () -> {
                    JsonNode guild = restClient.request("GET", "/guilds/" + guildId, null);
                    if (stateCache != null) {
                        stateCache.putGuild(guild);
                    }
                    return guild;
                }
        );
    }

    public JsonNode deleteMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return restClient.request("DELETE", "/channels/" + channelId + "/messages/" + messageId, null);
    }

    public JsonNode sendMessage(String channelId, DiscordMessage message) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        return restClient.sendMessage(channelId, message.toPayload());
    }

    public JsonNode sendMessageWithAttachments(String channelId, DiscordMessage message, List<DiscordAttachment> attachments) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        return restClient.sendMessageWithAttachments(channelId, message.toPayload(), attachments);
    }

    // Roles
    public JsonNode listGuildRoles(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getGuildRoles(guildId)),
                () -> {
                    JsonNode roles = restClient.request("GET", "/guilds/" + guildId + "/roles", null);
                    if (stateCache != null) {
                        stateCache.putGuildRoles(guildId, roles);
                    }
                    return roles;
                }
        );
    }

    public JsonNode createGuildRole(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        JsonNode result = restClient.request("POST", "/guilds/" + guildId + "/roles", payload);
        invalidateGuildScopedCaches(guildId);
        return result;
    }

    public JsonNode modifyGuildRole(String guildId, String roleId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(roleId, "roleId");
        Objects.requireNonNull(payload, "payload");
        JsonNode result = restClient.request("PATCH", "/guilds/" + guildId + "/roles/" + roleId, payload);
        if (stateCache != null) {
            stateCache.invalidateGuildRoles(guildId);
        }
        return result;
    }

    public JsonNode deleteGuildRole(String guildId, String roleId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(roleId, "roleId");
        JsonNode result = restClient.request("DELETE", "/guilds/" + guildId + "/roles/" + roleId, null);
        if (stateCache != null) {
            stateCache.invalidateGuildRoles(guildId);
        }
        return result;
    }

    // Emojis
    public JsonNode listGuildEmojis(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getGuildEmojis(guildId)),
                () -> {
                    JsonNode emojis = restClient.request("GET", "/guilds/" + guildId + "/emojis", null);
                    if (stateCache != null) {
                        stateCache.putGuildEmojis(guildId, emojis);
                    }
                    return emojis;
                }
        );
    }

    public JsonNode createGuildEmoji(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        JsonNode result = restClient.request("POST", "/guilds/" + guildId + "/emojis", payload);
        if (stateCache != null) {
            stateCache.invalidateGuildEmojis(guildId);
        }
        return result;
    }

    public JsonNode modifyGuildEmoji(String guildId, String emojiId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(emojiId, "emojiId");
        Objects.requireNonNull(payload, "payload");
        JsonNode result = restClient.request("PATCH", "/guilds/" + guildId + "/emojis/" + emojiId, payload);
        if (stateCache != null) {
            stateCache.invalidateGuildEmojis(guildId);
        }
        return result;
    }

    public JsonNode deleteGuildEmoji(String guildId, String emojiId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(emojiId, "emojiId");
        JsonNode result = restClient.request("DELETE", "/guilds/" + guildId + "/emojis/" + emojiId, null);
        if (stateCache != null) {
            stateCache.invalidateGuildEmojis(guildId);
        }
        return result;
    }

    // Webhooks
    public JsonNode listChannelWebhooks(String channelId) {
        requireNonBlank(channelId, "channelId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getChannelWebhooks(channelId)),
                () -> {
                    JsonNode webhooks = restClient.request("GET", "/channels/" + channelId + "/webhooks", null);
                    if (stateCache != null) {
                        stateCache.putChannelWebhooks(channelId, webhooks);
                    }
                    return webhooks;
                }
        );
    }

    public JsonNode listGuildWebhooks(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getGuildWebhooks(guildId)),
                () -> {
                    JsonNode webhooks = restClient.request("GET", "/guilds/" + guildId + "/webhooks", null);
                    if (stateCache != null) {
                        stateCache.putGuildWebhooks(guildId, webhooks);
                    }
                    return webhooks;
                }
        );
    }

    public JsonNode createWebhook(String channelId, String name) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(name, "name");
        JsonNode result = restClient.request("POST", "/channels/" + channelId + "/webhooks", Map.of("name", name));
        if (stateCache != null) {
            stateCache.invalidateChannelWebhooks(channelId);
        }
        return result;
    }

    public JsonNode deleteWebhook(String webhookId) {
        requireNonBlank(webhookId, "webhookId");
        return restClient.request("DELETE", "/webhooks/" + webhookId, null);
    }

    public JsonNode executeWebhook(String webhookId, String webhookToken, String content) {
        requireNonBlank(webhookId, "webhookId");
        requireNonBlank(webhookToken, "webhookToken");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", Objects.requireNonNull(content, "content"));
        return restClient.request("POST", "/webhooks/" + webhookId + "/" + webhookToken, payload);
    }

    // Threads
    public JsonNode startThreadFromMessage(String channelId, String messageId, Map<String, Object> payload) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/channels/" + channelId + "/messages/" + messageId + "/threads", payload);
    }

    public JsonNode startThreadWithoutMessage(String channelId, Map<String, Object> payload) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/channels/" + channelId + "/threads", payload);
    }

    public JsonNode joinThread(String threadId) {
        requireNonBlank(threadId, "threadId");
        return restClient.request("PUT", "/channels/" + threadId + "/thread-members/@me", null);
    }

    public JsonNode listPublicArchivedThreads(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.request("GET", "/channels/" + channelId + "/threads/archived/public", null);
    }

    // Scheduled Events
    public JsonNode listGuildScheduledEvents(String guildId, boolean withUserCount) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                cache(() -> stateCache == null ? Optional.empty() : stateCache.getScheduledEvents(guildId)),
                () -> {
                    JsonNode events = restClient.request(
                            "GET",
                            "/guilds/" + guildId + "/scheduled-events?with_user_count=" + withUserCount,
                            null
                    );
                    if (stateCache != null) {
                        stateCache.putScheduledEvents(guildId, events);
                    }
                    return events;
                }
        );
    }

    public JsonNode createGuildScheduledEvent(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        JsonNode result = restClient.request("POST", "/guilds/" + guildId + "/scheduled-events", payload);
        if (stateCache != null) {
            stateCache.invalidateScheduledEvents(guildId);
        }
        return result;
    }

    public JsonNode modifyGuildScheduledEvent(String guildId, String eventId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(eventId, "eventId");
        Objects.requireNonNull(payload, "payload");
        JsonNode result = restClient.request("PATCH", "/guilds/" + guildId + "/scheduled-events/" + eventId, payload);
        if (stateCache != null) {
            stateCache.invalidateScheduledEvents(guildId);
        }
        return result;
    }

    public JsonNode deleteGuildScheduledEvent(String guildId, String eventId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(eventId, "eventId");
        JsonNode result = restClient.request("DELETE", "/guilds/" + guildId + "/scheduled-events/" + eventId, null);
        if (stateCache != null) {
            stateCache.invalidateScheduledEvents(guildId);
        }
        return result;
    }

    public JsonNode request(String method, String path, Map<String, Object> body) {
        requireNonBlank(method, "method");
        requireNonBlank(path, "path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        return restClient.request(method, path, body);
    }

    private void invalidateGuildScopedCaches(String guildId) {
        if (stateCache != null) {
            stateCache.invalidateGuildRoles(guildId);
            stateCache.invalidateGuildEmojis(guildId);
            stateCache.invalidateGuildWebhooks(guildId);
            stateCache.invalidateScheduledEvents(guildId);
        }
    }
}
