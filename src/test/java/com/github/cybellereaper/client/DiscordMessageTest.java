package com.github.cybellereaper.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordMessageTest {

    @Test
    void toPayloadIncludesEphemeralFlagForEphemeralMessage() {
        DiscordMessage message = DiscordMessage.ofContent("hidden").asEphemeral();

        Map<String, Object> payload = message.toPayload();

        assertEquals("hidden", payload.get("content"));
        assertEquals(64, payload.get("flags"));
    }

    @Test
    void toPayloadFiltersOutNullAndEmptyEmbeds() {
        DiscordMessage message = new DiscordMessage(
                "hello",
                java.util.Arrays.asList(null, new DiscordEmbed("Title", null, null), new DiscordEmbed(" ", "", null)),
                false
        );

        Map<String, Object> payload = message.toPayload();

        assertEquals("hello", payload.get("content"));
        assertTrue(payload.containsKey("embeds"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> embeds = (List<Map<String, Object>>) payload.get("embeds");
        assertEquals(1, embeds.size());
        assertEquals("Title", embeds.get(0).get("title"));
    }
}
