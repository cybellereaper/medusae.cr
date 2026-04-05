package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.http.DiscordRestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * High-level REST API helper for common Discord resources.
 */
public final class DiscordApi {
    private final DiscordRestClient restClient;

    DiscordApi(DiscordRestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    public JsonNode getCurrentApplication() {
        return restClient.getCurrentApplication();
    }

    public JsonNode getCurrentUser() {
        return restClient.request("GET", "/users/@me", null);
    }

    public JsonNode getChannel(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.request("GET", "/channels/" + channelId, null);
    }

    public JsonNode getGuild(String guildId) {
        requireNonBlank(guildId, "guildId");
        return restClient.request("GET", "/guilds/" + guildId, null);
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
        return restClient.request("GET", "/guilds/" + guildId + "/roles", null);
    }

    public JsonNode createGuildRole(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/guilds/" + guildId + "/roles", payload);
    }

    public JsonNode modifyGuildRole(String guildId, String roleId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(roleId, "roleId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("PATCH", "/guilds/" + guildId + "/roles/" + roleId, payload);
    }

    public JsonNode deleteGuildRole(String guildId, String roleId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(roleId, "roleId");
        return restClient.request("DELETE", "/guilds/" + guildId + "/roles/" + roleId, null);
    }

    // Emojis
    public JsonNode listGuildEmojis(String guildId) {
        requireNonBlank(guildId, "guildId");
        return restClient.request("GET", "/guilds/" + guildId + "/emojis", null);
    }

    public JsonNode createGuildEmoji(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/guilds/" + guildId + "/emojis", payload);
    }

    public JsonNode modifyGuildEmoji(String guildId, String emojiId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(emojiId, "emojiId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("PATCH", "/guilds/" + guildId + "/emojis/" + emojiId, payload);
    }

    public JsonNode deleteGuildEmoji(String guildId, String emojiId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(emojiId, "emojiId");
        return restClient.request("DELETE", "/guilds/" + guildId + "/emojis/" + emojiId, null);
    }

    // Webhooks
    public JsonNode listChannelWebhooks(String channelId) {
        requireNonBlank(channelId, "channelId");
        return restClient.request("GET", "/channels/" + channelId + "/webhooks", null);
    }

    public JsonNode listGuildWebhooks(String guildId) {
        requireNonBlank(guildId, "guildId");
        return restClient.request("GET", "/guilds/" + guildId + "/webhooks", null);
    }

    public JsonNode createWebhook(String channelId, String name) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(name, "name");
        return restClient.request("POST", "/channels/" + channelId + "/webhooks", Map.of("name", name));
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
        return restClient.request("GET", "/guilds/" + guildId + "/scheduled-events?with_user_count=" + withUserCount, null);
    }

    public JsonNode createGuildScheduledEvent(String guildId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("POST", "/guilds/" + guildId + "/scheduled-events", payload);
    }

    public JsonNode modifyGuildScheduledEvent(String guildId, String eventId, Map<String, Object> payload) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(eventId, "eventId");
        Objects.requireNonNull(payload, "payload");
        return restClient.request("PATCH", "/guilds/" + guildId + "/scheduled-events/" + eventId, payload);
    }

    public JsonNode deleteGuildScheduledEvent(String guildId, String eventId) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(eventId, "eventId");
        return restClient.request("DELETE", "/guilds/" + guildId + "/scheduled-events/" + eventId, null);
    }

    public JsonNode request(String method, String path, Map<String, Object> body) {
        requireNonBlank(method, "method");
        requireNonBlank(path, "path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        return restClient.request(method, path, body);
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
