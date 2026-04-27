package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordStateCacheTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesGuildChannelAndMemberSnapshots() throws Exception {
        DiscordStateCache cache = new DiscordStateCache();

        cache.putGuild(objectMapper.readValue("{\"id\":\"1\",\"name\":\"guild\"}", GuildSnapshot.class));
        cache.putChannel(objectMapper.readValue("{\"id\":\"2\",\"name\":\"general\"}", ChannelSnapshot.class));
        cache.putMember(objectMapper.readValue("{\"guild_id\":\"1\",\"user\":{\"id\":\"99\"}}", GuildMemberSnapshot.class));

        assertTrue(cache.getGuild("1").isPresent());
        assertTrue(cache.getChannel("2").isPresent());
        assertTrue(cache.getMember("1", "99").isPresent());
    }

    @Test
    void removesMemberByCompositeKey() throws Exception {
        DiscordStateCache cache = new DiscordStateCache();
        cache.putMember(objectMapper.readValue("{\"guild_id\":\"1\",\"user\":{\"id\":\"99\"}}", GuildMemberSnapshot.class));

        cache.removeMember("1", "99");
        assertFalse(cache.getMember("1", "99").isPresent());
    }

    @Test
    void supportsAdditionalGuildScopedCollectionCaches() throws Exception {
        DiscordStateCache cache = new DiscordStateCache();

        List<Map<String, Object>> rows = List.of(Map.of("id", "row-1"));
        cache.putGuildRoles("1", rows);
        cache.putGuildEmojis("1", rows);
        cache.putGuildWebhooks("1", rows);
        cache.putScheduledEvents("1", rows);
        cache.putChannelWebhooks("2", rows);

        assertTrue(cache.getGuildRoles("1").isPresent());
        assertTrue(cache.getGuildEmojis("1").isPresent());
        assertTrue(cache.getGuildWebhooks("1").isPresent());
        assertTrue(cache.getScheduledEvents("1").isPresent());
        assertTrue(cache.getChannelWebhooks("2").isPresent());

        cache.removeGuild("1");
        cache.removeChannel("2");

        assertFalse(cache.getGuildRoles("1").isPresent());
        assertFalse(cache.getGuildEmojis("1").isPresent());
        assertFalse(cache.getGuildWebhooks("1").isPresent());
        assertFalse(cache.getScheduledEvents("1").isPresent());
        assertFalse(cache.getChannelWebhooks("2").isPresent());
    }
}
