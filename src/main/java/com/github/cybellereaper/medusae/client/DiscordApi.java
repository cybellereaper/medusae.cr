package com.github.cybellereaper.medusae.client;

import com.github.cybellereaper.medusae.http.DiscordApplication;
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

    private static <T> T getOrLoad(Optional<T> cached, Supplier<T> loader) {
        return cached.orElseGet(loader);
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public DiscordApplication getCurrentApplication() {
        return restClient.getCurrentApplication();
    }

    public Map<String, Object> getCurrentUser() {
        return restClient.request("GET", "/users/@me", null);
    }

    public ChannelSnapshot getChannel(String channelId) {
        requireNonBlank(channelId, "channelId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getChannel(channelId),
                () -> {
                    ChannelSnapshot channel = restClient.getChannel(channelId);
                    if (stateCache != null) {
                        stateCache.putChannel(channel);
                    }
                    return channel;
                }
        );
    }

    public GuildSnapshot getGuild(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getGuild(guildId),
                () -> {
                    GuildSnapshot guild = restClient.getGuild(guildId);
                    if (stateCache != null) {
                        stateCache.putGuild(guild);
                    }
                    return guild;
                }
        );
    }

    public Map<String, Object> deleteMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return restClient.request("DELETE", "/channels/" + channelId + "/messages/" + messageId, null);
    }

    public Map<String, Object> getMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return restClient.getMessage(channelId, messageId);
    }

    public Map<String, Object> editMessage(String channelId, String messageId, DiscordMessage message) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        Objects.requireNonNull(message, "message");
        return restClient.editMessage(channelId, messageId, message.toPayload());
    }

    public List<Map<String, Object>> getChannelMessages(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.getChannelMessages(channelId, Map.of());
    }

    public List<Map<String, Object>> getChannelMessages(String channelId, Map<String, Object> query) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(query, "query");
        return restClient.getChannelMessages(channelId, query);
    }

    public Map<String, Object> sendMessage(String channelId, DiscordMessage message) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        return restClient.sendMessage(channelId, message.toPayload());
    }

    public Map<String, Object> sendMessageWithAttachments(String channelId, DiscordMessage message, List<DiscordAttachment> attachments) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        return restClient.sendMessageWithAttachments(channelId, message.toPayload(), attachments);
    }

    public Map<String, Object> addReaction(String channelId, String messageId, String emoji) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        return restClient.addReaction(channelId, messageId, emoji);
    }

    public Map<String, Object> removeOwnReaction(String channelId, String messageId, String emoji) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        return restClient.removeOwnReaction(channelId, messageId, emoji);
    }

    public Map<String, Object> removeUserReaction(String channelId, String messageId, String emoji, String userId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        requireNonBlank(userId, "userId");
        return restClient.removeUserReaction(channelId, messageId, emoji, userId);
    }

    public Map<String, Object> clearMessageReactionEmoji(String channelId, String messageId, String emoji) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        return restClient.clearMessageReactionEmoji(channelId, messageId, emoji);
    }

    public Map<String, Object> clearMessageReactions(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return restClient.clearMessageReactions(channelId, messageId);
    }

    public Map<String, Object> pinMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return restClient.pinMessage(channelId, messageId);
    }

    public Map<String, Object> unpinMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return restClient.unpinMessage(channelId, messageId);
    }

    public List<Map<String, Object>> listPinnedMessages(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.listPinnedMessages(channelId);
    }

    public Map<String, Object> triggerTypingIndicator(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.triggerTypingIndicator(channelId);
    }

    // Roles
    public List<Map<String, Object>> listGuildRoles(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getGuildRoles(guildId),
                () -> {
                    List<Map<String, Object>> roles = restClient.requestList("GET", "/guilds/" + guildId + "/roles", null);
                    if (stateCache != null) {
                        stateCache.putGuildRoles(guildId, roles);
                    }
                    return roles;
                }
        );
    }

    public Map<String, Object> createGuildRole(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> result = restClient.request("POST", "/guilds/" + guildId + "/roles", payload);
        invalidateGuildScopedCaches(guildId);
        return result;
    }

    public Map<String, Object> modifyGuildRole(String guildId, String roleId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(roleId, "roleId");
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> result = restClient.request("PATCH", "/guilds/" + guildId + "/roles/" + roleId, payload);
        if (stateCache != null) {
            stateCache.invalidateGuildRoles(guildId);
        }
        return result;
    }

    public Map<String, Object> deleteGuildRole(String guildId, String roleId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(roleId, "roleId");
        Map<String, Object> result = restClient.request("DELETE", "/guilds/" + guildId + "/roles/" + roleId, null);
        if (stateCache != null) {
            stateCache.invalidateGuildRoles(guildId);
        }
        return result;
    }

    // Emojis
    public List<Map<String, Object>> listGuildEmojis(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getGuildEmojis(guildId),
                () -> {
                    List<Map<String, Object>> emojis = restClient.requestList("GET", "/guilds/" + guildId + "/emojis", null);
                    if (stateCache != null) {
                        stateCache.putGuildEmojis(guildId, emojis);
                    }
                    return emojis;
                }
        );
    }

    public Map<String, Object> createGuildEmoji(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> result = restClient.request("POST", "/guilds/" + guildId + "/emojis", payload);
        if (stateCache != null) {
            stateCache.invalidateGuildEmojis(guildId);
        }
        return result;
    }

    public Map<String, Object> modifyGuildEmoji(String guildId, String emojiId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(emojiId, "emojiId");
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> result = restClient.request("PATCH", "/guilds/" + guildId + "/emojis/" + emojiId, payload);
        if (stateCache != null) {
            stateCache.invalidateGuildEmojis(guildId);
        }
        return result;
    }

    public Map<String, Object> deleteGuildEmoji(String guildId, String emojiId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(emojiId, "emojiId");
        Map<String, Object> result = restClient.request("DELETE", "/guilds/" + guildId + "/emojis/" + emojiId, null);
        if (stateCache != null) {
            stateCache.invalidateGuildEmojis(guildId);
        }
        return result;
    }

    // Webhooks
    public List<Map<String, Object>> listChannelWebhooks(String channelId) {
        requireNonBlank(channelId, "channelId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getChannelWebhooks(channelId),
                () -> {
                    List<Map<String, Object>> webhooks = restClient.requestList("GET", "/channels/" + channelId + "/webhooks", null);
                    if (stateCache != null) {
                        stateCache.putChannelWebhooks(channelId, webhooks);
                    }
                    return webhooks;
                }
        );
    }

    public List<Map<String, Object>> listGuildWebhooks(String guildId) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getGuildWebhooks(guildId),
                () -> {
                    List<Map<String, Object>> webhooks = restClient.requestList("GET", "/guilds/" + guildId + "/webhooks", null);
                    if (stateCache != null) {
                        stateCache.putGuildWebhooks(guildId, webhooks);
                    }
                    return webhooks;
                }
        );
    }

    public Map<String, Object> createWebhook(String channelId, String name) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(name, "name");
        Map<String, Object> result = restClient.request("POST", "/channels/" + channelId + "/webhooks", Map.of("name", name));
        if (stateCache != null) {
            stateCache.invalidateChannelWebhooks(channelId);
        }
        return result;
    }

    public Map<String, Object> deleteWebhook(String webhookId) {
        requireNonBlank(webhookId, "webhookId");
        return restClient.request("DELETE", "/webhooks/" + webhookId, null);
    }

    public Map<String, Object> executeWebhook(String webhookId, String webhookToken, String content) {
        requireNonBlank(webhookId, "webhookId");
        requireNonBlank(webhookToken, "webhookToken");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("content", Objects.requireNonNull(content, "content"));
        return restClient.request("POST", "/webhooks/" + webhookId + "/" + webhookToken, payload);
    }

    // Threads
    public Map<String, Object> startThreadFromMessage(String channelId, String messageId, Map<String, Object> payload) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/channels/" + channelId + "/messages/" + messageId + "/threads", payload);
    }

    public Map<String, Object> startThreadWithoutMessage(String channelId, Map<String, Object> payload) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/channels/" + channelId + "/threads", payload);
    }

    public Map<String, Object> joinThread(String threadId) {
        requireNonBlank(threadId, "threadId");
        return restClient.request("PUT", "/channels/" + threadId + "/thread-members/@me", null);
    }

    public Map<String, Object> listPublicArchivedThreads(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.request("GET", "/channels/" + channelId + "/threads/archived/public", null);
    }

    // Scheduled Events
    public List<Map<String, Object>> listGuildScheduledEvents(String guildId, boolean withUserCount) {
        requireNonBlank(guildId, "guildId");
        return getOrLoad(
                stateCache == null ? Optional.empty() : stateCache.getScheduledEvents(guildId),
                () -> {
                    List<Map<String, Object>> events = restClient.requestList(
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

    public Map<String, Object> createGuildScheduledEvent(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> result = restClient.request("POST", "/guilds/" + guildId + "/scheduled-events", payload);
        if (stateCache != null) {
            stateCache.invalidateScheduledEvents(guildId);
        }
        return result;
    }

    public Map<String, Object> modifyGuildScheduledEvent(String guildId, String eventId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(eventId, "eventId");
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> result = restClient.request("PATCH", "/guilds/" + guildId + "/scheduled-events/" + eventId, payload);
        if (stateCache != null) {
            stateCache.invalidateScheduledEvents(guildId);
        }
        return result;
    }

    public Map<String, Object> deleteGuildScheduledEvent(String guildId, String eventId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(eventId, "eventId");
        Map<String, Object> result = restClient.request("DELETE", "/guilds/" + guildId + "/scheduled-events/" + eventId, null);
        if (stateCache != null) {
            stateCache.invalidateScheduledEvents(guildId);
        }
        return result;
    }

    public Map<String, Object> request(String method, String path, Map<String, Object> body) {
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
