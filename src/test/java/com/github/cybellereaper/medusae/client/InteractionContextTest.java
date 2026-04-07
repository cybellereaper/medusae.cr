package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InteractionContextTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void readsNestedSlashCommandOptions() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "id": "1",
                  "token": "abc",
                  "type": 2,
                  "data": {
                    "name": "admin",
                    "options": [
                      {
                        "name": "user",
                        "options": [
                          { "name": "target", "value": "alice" }
                        ]
                      }
                    ]
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(interaction, (id, token, type, data) -> {
        });

        assertEquals("alice", context.optionString("target"));
        assertNull(context.optionString("missing"));
        assertEquals("alice", context.requiredOptionString("target"));
        assertThrows(IllegalArgumentException.class, () -> context.requiredOptionString("missing"));
    }

    @Test
    void validatesAutocompleteChoiceLimit() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "id": "1",
                  "token": "abc",
                  "type": 4,
                  "data": {
                    "name": "echo"
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(interaction, (id, token, type, data) -> {
        });
        List<AutocompleteChoice> choices = java.util.stream.IntStream.range(0, 26)
                .mapToObj(i -> new AutocompleteChoice("choice-" + i, "value-" + i))
                .toList();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> context.respondWithAutocompleteChoices(choices));
        assertTrue(ex.getMessage().contains("at most 25"));
    }

    @Test
    void supportsDefersAndBasicMetadata() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "id": "9",
                  "token": "xyz",
                  "type": 5,
                  "data": {
                    "custom_id": "feedback_modal"
                  }
                }
                """);

        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicInteger responseCount = new AtomicInteger();
        InteractionContext context = InteractionContext.from(interaction, (id, token, type, data) -> {
            responseType.set(type);
            responseCount.incrementAndGet();
        });

        assertEquals("9", context.id());
        assertEquals("xyz", context.token());
        assertEquals("feedback_modal", context.customId());
        assertEquals(5, context.interactionType());

        context.deferUpdate();
        assertEquals(6, responseType.get());
        assertEquals(1, responseCount.get());
    }

    @Test
    void parsesNumericAndBooleanOptionsSafely() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "id": "55",
                  "token": "abc",
                  "type": 2,
                  "data": {
                    "type": 1,
                    "name": "config",
                    "options": [
                      { "name": "count", "value": 42 },
                      { "name": "enabled", "value": true },
                      { "name": "text_num", "value": "77" },
                      { "name": "text_bool", "value": "false" },
                      { "name": "invalid_num", "value": "x42" }
                    ]
                  },
                  "guild_id": "guild-1",
                  "channel_id": "chan-9",
                  "member": {
                    "user": {
                      "id": "user-3"
                    }
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(interaction, (id, token, type, data) -> {
        });

        assertEquals(42L, context.optionLong("count"));
        assertEquals(42, context.optionInt("count"));
        assertEquals(77L, context.optionLong("text_num"));
        assertEquals(77.0, context.optionDouble("text_num"));
        assertNull(context.optionLong("invalid_num"));
        assertNull(context.optionDouble("invalid_num"));
        assertEquals(true, context.optionBoolean("enabled"));
        assertEquals(false, context.optionBoolean("text_bool"));
        assertNull(context.optionBoolean("missing"));
        assertEquals("guild-1", context.guildId());
        assertEquals("chan-9", context.channelId());
        assertEquals("user-3", context.userId());
        assertEquals(1, context.commandType());
    }

    @Test
    void resolvesEntitiesFromResolvedInteractionData() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "id": "88",
                  "token": "tok",
                  "type": 2,
                  "data": {
                    "name": "profile",
                    "options": [
                      { "name": "photo", "value": "att-1" },
                      { "name": "target_user", "value": "user-1" },
                      { "name": "target_role", "value": "role-1" },
                      { "name": "target_channel", "value": "chan-1" }
                    ],
                    "resolved": {
                      "attachments": {
                        "att-1": {
                          "id": "att-1",
                          "filename": "avatar.png",
                          "content_type": "image/png",
                          "size": 12345,
                          "url": "https://cdn/att-1",
                          "proxy_url": "https://proxy/att-1"
                        }
                      },
                      "users": {
                        "user-1": { "id": "user-1", "username": "neo", "global_name": "The One", "bot": false }
                      },
                      "members": {
                        "user-1": { "nick": "chosen" }
                      },
                      "roles": {
                        "role-1": { "id": "role-1", "name": "admin", "color": 16711680 }
                      },
                      "channels": {
                        "chan-1": { "id": "chan-1", "name": "general", "type": 0 }
                      }
                    }
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(interaction, (id, token, type, data) -> {
        });

        assertEquals("avatar.png", context.optionResolvedAttachment("photo").path("filename").asText());
        assertEquals("neo", context.optionResolvedUser("target_user").path("username").asText());
        assertEquals("admin", context.optionResolvedRole("target_role").path("name").asText());
        assertEquals("general", context.optionResolvedChannel("target_channel").path("name").asText());

        ResolvedAttachment attachment = context.optionResolvedAttachmentValue("photo");
        assertEquals("att-1", attachment.id());
        assertEquals("image/png", attachment.contentType());
        assertEquals(12345L, attachment.size());

        ResolvedUser user = context.optionResolvedUserValue("target_user");
        assertEquals("The One", user.globalName());
        assertEquals(false, user.bot());

        ResolvedMember member = context.resolvedMemberValue("user-1");
        assertEquals("chosen", member.nickname());

        ResolvedRole role = context.optionResolvedRoleValue("target_role");
        assertEquals(16711680, role.color());

        ResolvedChannel channel = context.optionResolvedChannelValue("target_channel");
        assertEquals(0, channel.type());

        assertNull(context.optionResolvedAttachment("missing"));
        assertNull(context.optionResolvedUserValue("missing"));
    }
}
