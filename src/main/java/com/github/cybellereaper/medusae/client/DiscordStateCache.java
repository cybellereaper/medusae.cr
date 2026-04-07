package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional in-memory cache for gateway snapshots and read-mostly REST collections.
 */
public final class DiscordStateCache {
    private final Map<String, JsonNode> guildsById = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> channelsById = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> membersByCompositeKey = new ConcurrentHashMap<>();

    private final Map<String, JsonNode> guildRolesByGuildId = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> guildEmojisByGuildId = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> guildWebhooksByGuildId = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> channelWebhooksByChannelId = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> scheduledEventsByGuildId = new ConcurrentHashMap<>();

    private static String memberKey(String guildId, String userId) {
        return guildId + ":" + userId;
    }

    private static String idOf(JsonNode node) {
        return node.path("id").asText("");
    }

    private static void putCollection(Map<String, JsonNode> collectionCache, String key, JsonNode value) {
        if (key != null && !key.isBlank() && value != null) {
            collectionCache.put(key, value.deepCopy());
        }
    }

    public void putGuild(JsonNode guild) {
        String id = idOf(guild);
        if (!id.isBlank()) {
            guildsById.put(id, guild.deepCopy());
        }
    }

    public void putChannel(JsonNode channel) {
        String id = idOf(channel);
        if (!id.isBlank()) {
            channelsById.put(id, channel.deepCopy());
        }
    }

    public void putMember(JsonNode eventPayload) {
        String guildId = eventPayload.path("guild_id").asText("");
        JsonNode user = eventPayload.path("user");
        String userId = idOf(user);
        if (!guildId.isBlank() && !userId.isBlank()) {
            membersByCompositeKey.put(memberKey(guildId, userId), eventPayload.deepCopy());
        }
    }

    public void removeGuild(String guildId) {
        if (!guildId.isBlank()) {
            guildsById.remove(guildId);
            guildRolesByGuildId.remove(guildId);
            guildEmojisByGuildId.remove(guildId);
            guildWebhooksByGuildId.remove(guildId);
            scheduledEventsByGuildId.remove(guildId);
        }
    }

    public void removeChannel(String channelId) {
        if (!channelId.isBlank()) {
            channelsById.remove(channelId);
            channelWebhooksByChannelId.remove(channelId);
        }
    }

    public void removeMember(String guildId, String userId) {
        if (!guildId.isBlank() && !userId.isBlank()) {
            membersByCompositeKey.remove(memberKey(guildId, userId));
        }
    }

    public Optional<JsonNode> getGuild(String guildId) {
        return Optional.ofNullable(guildsById.get(guildId));
    }

    public Optional<JsonNode> getChannel(String channelId) {
        return Optional.ofNullable(channelsById.get(channelId));
    }

    public Optional<JsonNode> getMember(String guildId, String userId) {
        return Optional.ofNullable(membersByCompositeKey.get(memberKey(guildId, userId)));
    }

    public Optional<JsonNode> getGuildRoles(String guildId) {
        return Optional.ofNullable(guildRolesByGuildId.get(guildId));
    }

    public void putGuildRoles(String guildId, JsonNode roles) {
        putCollection(guildRolesByGuildId, guildId, roles);
    }

    public void invalidateGuildRoles(String guildId) {
        guildRolesByGuildId.remove(guildId);
    }

    public Optional<JsonNode> getGuildEmojis(String guildId) {
        return Optional.ofNullable(guildEmojisByGuildId.get(guildId));
    }

    public void putGuildEmojis(String guildId, JsonNode emojis) {
        putCollection(guildEmojisByGuildId, guildId, emojis);
    }

    public void invalidateGuildEmojis(String guildId) {
        guildEmojisByGuildId.remove(guildId);
    }

    public Optional<JsonNode> getGuildWebhooks(String guildId) {
        return Optional.ofNullable(guildWebhooksByGuildId.get(guildId));
    }

    public void putGuildWebhooks(String guildId, JsonNode webhooks) {
        putCollection(guildWebhooksByGuildId, guildId, webhooks);
    }

    public void invalidateGuildWebhooks(String guildId) {
        guildWebhooksByGuildId.remove(guildId);
    }

    public Optional<JsonNode> getChannelWebhooks(String channelId) {
        return Optional.ofNullable(channelWebhooksByChannelId.get(channelId));
    }

    public void putChannelWebhooks(String channelId, JsonNode webhooks) {
        putCollection(channelWebhooksByChannelId, channelId, webhooks);
    }

    public void invalidateChannelWebhooks(String channelId) {
        channelWebhooksByChannelId.remove(channelId);
    }

    public Optional<JsonNode> getScheduledEvents(String guildId) {
        return Optional.ofNullable(scheduledEventsByGuildId.get(guildId));
    }

    public void putScheduledEvents(String guildId, JsonNode events) {
        putCollection(scheduledEventsByGuildId, guildId, events);
    }

    public void invalidateScheduledEvents(String guildId) {
        scheduledEventsByGuildId.remove(guildId);
    }
}
