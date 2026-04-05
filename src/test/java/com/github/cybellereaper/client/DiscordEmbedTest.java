package com.github.cybellereaper.client;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordEmbedTest {

    @Test
    void toPayloadIncludesImageAndThumbnailWhenProvided() {
        DiscordEmbed embed = new DiscordEmbed("Title", "Description", 0xFFFFFF)
                .withUrl("https://example.com")
                .withImage("https://example.com/image.png")
                .withThumbnail("https://example.com/thumb.png");

        Map<String, Object> payload = embed.toPayload();

        assertEquals("Title", payload.get("title"));
        assertEquals("Description", payload.get("description"));
        assertEquals(0xFFFFFF, payload.get("color"));
        assertEquals("https://example.com", payload.get("url"));
        assertEquals(Map.of("url", "https://example.com/image.png"), payload.get("image"));
        assertEquals(Map.of("url", "https://example.com/thumb.png"), payload.get("thumbnail"));
    }

    @Test
    void toPayloadSkipsBlankTextFields() {
        DiscordEmbed embed = new DiscordEmbed(" ", "", null)
                .withImage(" ")
                .withThumbnail(null)
                .withUrl(" ");

        Map<String, Object> payload = embed.toPayload();

        assertTrue(payload.isEmpty());
        assertFalse(payload.containsKey("image"));
    }
}
