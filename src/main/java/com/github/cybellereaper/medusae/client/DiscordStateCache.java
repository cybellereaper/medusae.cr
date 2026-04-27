package com.github.cybellereaper.medusae.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional in-memory cache for gateway snapshots and read-mostly REST collections.
 */
public final class DiscordStateCache {
    private final Map<String, GuildSnapshot> guildsById = new ConcurrentHashMap<>();
    private final Map<String, ChannelSnapshot> channelsById = new ConcurrentHashMap<>();
    private final Map<String, GuildMemberSnapshot> membersByCompositeKey = new ConcurrentHashMap<>();

    private final Map<String, List<Map<String, Object>>> guildRolesByGuildId = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> guildEmojisByGuildId = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> guildWebhooksByGuildId = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> channelWebhooksByChannelId = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> scheduledEventsByGuildId = new ConcurrentHashMap<>();

    private static String memberKey(String guildId, String userId) {
        return guildId + ":" + userId;
    }

    private static void putCollection(Map<String, List<Map<String, Object>>> collectionCache, String key, List<Map<String, Object>> value) {
        if (key != null && !key.isBlank() && value != null) {
            collectionCache.put(key, List.copyOf(value.stream().map(Map::copyOf).toList()));
        }
    }

    public void putGuild(GuildSnapshot guild) {
        if (guild != null && guild.id() != null && !guild.id().isBlank()) {
            guildsById.put(guild.id(), guild);
        }
    }

    public void putChannel(ChannelSnapshot channel) {
        if (channel != null && channel.id() != null && !channel.id().isBlank()) {
            channelsById.put(channel.id(), channel);
        }
    }

    public void putMember(GuildMemberSnapshot eventPayload) {
        String guildId = eventPayload == null ? "" : stringOrEmpty(eventPayload.guildId());
        String userId = eventPayload == null || eventPayload.user() == null ? "" : stringOrEmpty(eventPayload.user().id());
        if (!guildId.isBlank() && !userId.isBlank()) {
            membersByCompositeKey.put(memberKey(guildId, userId), eventPayload);
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

    public Optional<GuildSnapshot> getGuild(String guildId) {
        return Optional.ofNullable(guildsById.get(guildId));
    }

    public Optional<ChannelSnapshot> getChannel(String channelId) {
        return Optional.ofNullable(channelsById.get(channelId));
    }

    public Optional<GuildMemberSnapshot> getMember(String guildId, String userId) {
        return Optional.ofNullable(membersByCompositeKey.get(memberKey(guildId, userId)));
    }

    public Optional<List<Map<String, Object>>> getGuildRoles(String guildId) {
        return Optional.ofNullable(guildRolesByGuildId.get(guildId));
    }

    public void putGuildRoles(String guildId, List<Map<String, Object>> roles) {
        putCollection(guildRolesByGuildId, guildId, roles);
    }

    public void invalidateGuildRoles(String guildId) {
        guildRolesByGuildId.remove(guildId);
    }

    public Optional<List<Map<String, Object>>> getGuildEmojis(String guildId) {
        return Optional.ofNullable(guildEmojisByGuildId.get(guildId));
    }

    public void putGuildEmojis(String guildId, List<Map<String, Object>> emojis) {
        putCollection(guildEmojisByGuildId, guildId, emojis);
    }

    public void invalidateGuildEmojis(String guildId) {
        guildEmojisByGuildId.remove(guildId);
    }

    public Optional<List<Map<String, Object>>> getGuildWebhooks(String guildId) {
        return Optional.ofNullable(guildWebhooksByGuildId.get(guildId));
    }

    public void putGuildWebhooks(String guildId, List<Map<String, Object>> webhooks) {
        putCollection(guildWebhooksByGuildId, guildId, webhooks);
    }

    public void invalidateGuildWebhooks(String guildId) {
        guildWebhooksByGuildId.remove(guildId);
    }

    public Optional<List<Map<String, Object>>> getChannelWebhooks(String channelId) {
        return Optional.ofNullable(channelWebhooksByChannelId.get(channelId));
    }

    public void putChannelWebhooks(String channelId, List<Map<String, Object>> webhooks) {
        putCollection(channelWebhooksByChannelId, channelId, webhooks);
    }

    public void invalidateChannelWebhooks(String channelId) {
        channelWebhooksByChannelId.remove(channelId);
    }

    public Optional<List<Map<String, Object>>> getScheduledEvents(String guildId) {
        return Optional.ofNullable(scheduledEventsByGuildId.get(guildId));
    }

    public void putScheduledEvents(String guildId, List<Map<String, Object>> events) {
        putCollection(scheduledEventsByGuildId, guildId, events);
    }

    public void invalidateScheduledEvents(String guildId) {
        scheduledEventsByGuildId.remove(guildId);
    }

    private static String stringOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
