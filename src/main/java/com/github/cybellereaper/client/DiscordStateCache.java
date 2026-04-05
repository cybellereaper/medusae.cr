package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optional in-memory cache for select guild/channel/member state maintained from gateway events.
 */
public final class DiscordStateCache {
    private final Map<String, JsonNode> guildsById = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> channelsById = new ConcurrentHashMap<>();
    private final Map<String, JsonNode> membersByCompositeKey = new ConcurrentHashMap<>();

    public void putGuild(JsonNode guild) {
        String id = guild.path("id").asText("");
        if (!id.isBlank()) {
            guildsById.put(id, guild.deepCopy());
        }
    }

    public void putChannel(JsonNode channel) {
        String id = channel.path("id").asText("");
        if (!id.isBlank()) {
            channelsById.put(id, channel.deepCopy());
        }
    }

    public void putMember(JsonNode eventPayload) {
        String guildId = eventPayload.path("guild_id").asText("");
        JsonNode user = eventPayload.path("user");
        String userId = user.path("id").asText("");
        if (!guildId.isBlank() && !userId.isBlank()) {
            membersByCompositeKey.put(key(guildId, userId), eventPayload.deepCopy());
        }
    }

    public void removeMember(String guildId, String userId) {
        if (!guildId.isBlank() && !userId.isBlank()) {
            membersByCompositeKey.remove(key(guildId, userId));
        }
    }

    public Optional<JsonNode> getGuild(String guildId) {
        return Optional.ofNullable(guildsById.get(guildId));
    }

    public Optional<JsonNode> getChannel(String channelId) {
        return Optional.ofNullable(channelsById.get(channelId));
    }

    public Optional<JsonNode> getMember(String guildId, String userId) {
        return Optional.ofNullable(membersByCompositeKey.get(key(guildId, userId)));
    }

    private static String key(String guildId, String userId) {
        return guildId + ":" + userId;
    }
}
