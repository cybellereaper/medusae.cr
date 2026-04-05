package com.github.cybellereaper.client;

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
}
