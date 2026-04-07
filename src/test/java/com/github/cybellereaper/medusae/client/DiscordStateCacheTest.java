package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordStateCacheTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesGuildChannelAndMemberSnapshots() throws Exception {
        DiscordStateCache cache = new DiscordStateCache();

        cache.putGuild(objectMapper.readTree("{\"id\":\"1\",\"name\":\"guild\"}"));
        cache.putChannel(objectMapper.readTree("{\"id\":\"2\",\"name\":\"general\"}"));
        cache.putMember(objectMapper.readTree("{\"guild_id\":\"1\",\"user\":{\"id\":\"99\"}}"));

        assertTrue(cache.getGuild("1").isPresent());
        assertTrue(cache.getChannel("2").isPresent());
        assertTrue(cache.getMember("1", "99").isPresent());
    }

    @Test
    void removesMemberByCompositeKey() throws Exception {
        DiscordStateCache cache = new DiscordStateCache();
        cache.putMember(objectMapper.readTree("{\"guild_id\":\"1\",\"user\":{\"id\":\"99\"}}"));

        cache.removeMember("1", "99");
        assertFalse(cache.getMember("1", "99").isPresent());
    }

    @Test
    void supportsAdditionalGuildScopedCollectionCaches() throws Exception {
        DiscordStateCache cache = new DiscordStateCache();

        cache.putGuildRoles("1", objectMapper.readTree("[{\"id\":\"r1\"}]"));
        cache.putGuildEmojis("1", objectMapper.readTree("[{\"id\":\"e1\"}]"));
        cache.putGuildWebhooks("1", objectMapper.readTree("[{\"id\":\"w1\"}]"));
        cache.putScheduledEvents("1", objectMapper.readTree("[{\"id\":\"s1\"}]"));
        cache.putChannelWebhooks("2", objectMapper.readTree("[{\"id\":\"cw1\"}]"));

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
