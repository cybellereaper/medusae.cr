package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SlashCommandRouterTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void routesSlashCommandInteractionsToRegisteredHandlers() throws Exception {
        AtomicInteger invocationCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerSlashHandler("ping", ignored -> invocationCount.incrementAndGet());

        router.handleInteraction(interactionPayload(2, "ping", null, "42", "token-value", null, 1));

        assertEquals(1, invocationCount.get());
    }

    @Test
    void routesTypedSlashCommandWithContextAndTypedParameters() throws Exception {
        AtomicReference<String> commandName = new AtomicReference<>();
        AtomicReference<String> guildId = new AtomicReference<>();
        AtomicReference<String> channelId = new AtomicReference<>();
        AtomicReference<String> userId = new AtomicReference<>();
        AtomicReference<String> textValue = new AtomicReference<>();
        AtomicReference<Long> countValue = new AtomicReference<>();
        AtomicReference<Boolean> enabledValue = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerSlashContextHandler("echo", interaction -> {
            commandName.set(interaction.context().commandName());
            guildId.set(interaction.context().guildId());
            channelId.set(interaction.context().channelId());
            userId.set(interaction.context().userId());
            textValue.set(interaction.parameters().requireString("text"));
            countValue.set(interaction.parameters().getLong("count"));
            enabledValue.set(interaction.parameters().getBoolean("enabled"));
        });

        JsonNode payload = MAPPER.readTree("""
                {
                  "type": 2,
                  "id": "42",
                  "token": "token-value",
                  "guild_id": "guild-1",
                  "channel_id": "channel-1",
                  "member": {
                    "user": {
                      "id": "user-1",
                      "username": "bot-user"
                    }
                  },
                  "data": {
                    "type": 1,
                    "name": "echo",
                    "options": [
                      {"name": "text", "value": "hello"},
                      {"name": "count", "value": 5},
                      {"name": "enabled", "value": true}
                    ]
                  }
                }
                """);

        router.handleInteraction(payload);

        assertEquals("echo", commandName.get());
        assertEquals("guild-1", guildId.get());
        assertEquals("channel-1", channelId.get());
        assertEquals("user-1", userId.get());
        assertEquals("hello", textValue.get());
        assertEquals(5L, countValue.get());
        assertEquals(true, enabledValue.get());
    }

    @Test
    void invokesBothRawAndTypedHandlersWhenBothAreRegistered() throws Exception {
        AtomicInteger rawCount = new AtomicInteger(0);
        AtomicInteger typedCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerSlashHandler("echo", ignored -> rawCount.incrementAndGet());
        router.registerSlashContextHandler("echo", ignored -> typedCount.incrementAndGet());

        router.handleInteraction(interactionPayload(2, "echo", null, "42", "token", "hello", 1));

        assertEquals(1, rawCount.get());
        assertEquals(1, typedCount.get());
    }

    @Test
    void routesAutocompleteInteractionsByCommandName() throws Exception {
        AtomicInteger autocompleteCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerAutocompleteHandler("echo", ignored -> autocompleteCount.incrementAndGet());

        router.handleInteraction(interactionPayload(4, "echo", null, "42", "token-value", "he", null));

        assertEquals(1, autocompleteCount.get());
    }

    @Test
    void routesComponentAndModalInteractionsByCustomId() throws Exception {
        AtomicInteger componentCount = new AtomicInteger(0);
        AtomicInteger modalCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerComponentHandler("confirm_button", ignored -> componentCount.incrementAndGet());
        router.registerModalHandler("feedback_modal", ignored -> modalCount.incrementAndGet());

        router.handleInteraction(interactionPayload(3, null, "confirm_button", "1", "token", null, null));
        router.handleInteraction(interactionPayload(5, null, "feedback_modal", "2", "token", null, null));

        assertEquals(1, componentCount.get());
        assertEquals(1, modalCount.get());
    }

    @Test
    void routesTypedModalInteractionsWithParameterAccess() throws Exception {
        AtomicReference<String> modalId = new AtomicReference<>();
        AtomicReference<String> feedback = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerModalContextHandler("feedback_modal", interaction -> {
            modalId.set(interaction.context().customId());
            feedback.set(interaction.parameters().requireString("feedback"));
        });

        JsonNode payload = MAPPER.readTree("""
                {
                  "type": 5,
                  "id": "123",
                  "token": "abc",
                  "data": {
                    "custom_id": "feedback_modal",
                    "components": [
                      {
                        "type": 1,
                        "components": [
                          {
                            "type": 4,
                            "custom_id": "feedback",
                            "value": "Great bot"
                          }
                        ]
                      }
                    ]
                  }
                }
                """);

        router.handleInteraction(payload);

        assertEquals("feedback_modal", modalId.get());
        assertEquals("Great bot", feedback.get());
    }

    @Test
    void routesUserAndMessageContextMenusByCommandName() throws Exception {
        AtomicInteger userCount = new AtomicInteger(0);
        AtomicInteger messageCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerUserContextMenuHandler("Inspect User", ignored -> userCount.incrementAndGet());
        router.registerMessageContextMenuHandler("Quote Message", ignored -> messageCount.incrementAndGet());

        router.handleInteraction(interactionPayload(2, "Inspect User", null, "42", "token-value", null, 2));
        router.handleInteraction(interactionPayload(2, "Quote Message", null, "43", "token-value", null, 3));

        assertEquals(1, userCount.get());
        assertEquals(1, messageCount.get());
    }

    @Test
    void autoRespondsToPingInteractionsWithPong() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> responseType.set(type));

        router.handleInteraction(interactionPayload(1, null, null, "123", "abc", null, null));

        assertEquals(1, responseType.get());
    }

    @Test
    void respondEphemeralUsesCorrectFlags() throws Exception {
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> responseData.set(data));

        router.respondEphemeral(interactionPayload(2, "ping", null, "123", "abc", null, 1), "hidden");

        assertEquals("hidden", responseData.get().get("content"));
        assertEquals(64, responseData.get().get("flags"));
    }

    @Test
    void respondWithEmbedsIncludesEmbedPayload() throws Exception {
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> responseData.set(data));

        router.respondWithEmbeds(
                interactionPayload(2, "ping", null, "123", "abc", null, 1),
                "hello",
                List.of(new DiscordEmbed("Title", "Description", 12345))
        );

        assertEquals("hello", responseData.get().get("content"));
        assertTrue(responseData.get().containsKey("embeds"));
    }

    @Test
    void respondWithAutocompleteChoicesUsesCorrectResponseType() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });

        router.respondWithAutocompleteChoices(
                interactionPayload(4, "echo", null, "123", "abc", "he", null),
                List.of(new AutocompleteChoice("hello", "hello"))
        );

        assertEquals(8, responseType.get());
        assertTrue(responseData.get().containsKey("choices"));
    }

    @Test
    void respondWithModalUsesCorrectResponseType() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });

        DiscordModal modal = DiscordModal.of(
                "feedback_modal",
                "Feedback",
                List.of(DiscordActionRow.of(List.of(
                        DiscordTextInput.paragraph("feedback", "Feedback")
                )))
        );

        router.respondWithModal(interactionPayload(2, "ping", null, "123", "abc", null, 1), modal);

        assertEquals(9, responseType.get());
        assertEquals("feedback_modal", responseData.get().get("custom_id"));
    }

    @Test
    void returnsModalInputValueWhenPresent() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "type": 5,
                  "id": "123",
                  "token": "abc",
                  "data": {
                    "custom_id": "feedback_modal",
                    "components": [
                      {
                        "type": 1,
                        "components": [
                          {
                            "type": 4,
                            "custom_id": "feedback",
                            "value": "Great bot"
                          }
                        ]
                      }
                    ]
                  }
                }
                """);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });

        assertEquals("Great bot", router.getModalValue(interaction, "feedback"));
        assertNull(router.getModalValue(interaction, "missing"));
    }

    @Test
    void returnsStringOptionValueWhenPresent() throws Exception {
        JsonNode interaction = interactionPayload(2, "echo", null, "123", "abc", "hello", 1);
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });

        assertEquals("hello", router.getOptionString(interaction, "text"));
        assertNull(router.getOptionString(interaction, "missing"));
    }

    @Test
    void slashCommandParametersHandleTypeMismatchAndMissingRequiredValue() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "type": 2,
                  "id": "123",
                  "token": "abc",
                  "data": {
                    "type": 1,
                    "name": "test",
                    "options": [
                      {"name": "text", "value": "hello"},
                      {"name": "number", "value": 7}
                    ]
                  }
                }
                """);
        SlashCommandParameters parameters = new SlashCommandParameters(interaction);

        assertNull(parameters.getLong("text"));
        assertNull(parameters.getBoolean("number"));
        assertThrows(IllegalArgumentException.class, () -> parameters.requireString("missing"));
    }

    @Test
    void interactionContextResolvesTopLevelUserWhenMemberIsMissing() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "type": 3,
                  "id": "123",
                  "token": "abc",
                  "user": {
                    "id": "user-2",
                    "username": "top-level-user"
                  },
                  "data": {
                    "custom_id": "confirm_button"
                  }
                }
                """);

        InteractionContext context = new InteractionContext(interaction);

        assertEquals("user-2", context.userId());
        assertEquals("top-level-user", context.username());
    }

    @Test
    void typedParametersExposeResolvedEntitiesForIdBasedOptions() throws Exception {
        JsonNode interaction = MAPPER.readTree("""
                {
                  "type": 2,
                  "id": "123",
                  "token": "abc",
                  "data": {
                    "type": 1,
                    "name": "inspect",
                    "options": [
                      {"name": "target_user", "value": "u-1"},
                      {"name": "target_channel", "value": "c-1"},
                      {"name": "upload", "value": "a-1"}
                    ],
                    "resolved": {
                      "users": {
                        "u-1": {"id": "u-1", "username": "alice"}
                      },
                      "channels": {
                        "c-1": {"id": "c-1", "name": "general"}
                      },
                      "attachments": {
                        "a-1": {"id": "a-1", "filename": "report.txt"}
                      }
                    }
                  }
                }
                """);

        SlashCommandParameters parameters = new SlashCommandParameters(interaction);
        InteractionContext context = new InteractionContext(interaction);

        assertEquals("u-1", parameters.getId("target_user"));
        assertEquals("alice", parameters.getResolvedUser("target_user").path("username").asText());
        assertEquals("general", parameters.getResolvedChannel("target_channel").path("name").asText());
        assertEquals("report.txt", parameters.getResolvedAttachment("upload").path("filename").asText());

        assertEquals("alice", context.resolvedUser("u-1").path("username").asText());
        assertEquals("general", context.resolvedChannel("c-1").path("name").asText());
        assertEquals("report.txt", context.resolvedAttachment("a-1").path("filename").asText());
    }


    @Test
    void routesComponentInteractionsUsingPrefixHandlers() throws Exception {
        AtomicInteger rawCount = new AtomicInteger(0);
        AtomicInteger contextCount = new AtomicInteger(0);
        AtomicReference<String> routedCustomId = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerComponentPrefixHandler("ticket:", ignored -> rawCount.incrementAndGet());
        router.registerComponentPrefixContextHandler("ticket:", interaction -> {
            contextCount.incrementAndGet();
            routedCustomId.set(interaction.customId());
        });

        router.handleInteraction(interactionPayload(3, null, "ticket:close:42", "1", "token", null, null));

        assertEquals(1, rawCount.get());
        assertEquals(1, contextCount.get());
        assertEquals("ticket:close:42", routedCustomId.get());
    }

    @Test
    void prefersLongestPrefixWhenMultipleComponentPrefixesMatch() throws Exception {
        AtomicInteger broadPrefixCount = new AtomicInteger(0);
        AtomicInteger specificPrefixCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerComponentPrefixHandler("ticket:", ignored -> broadPrefixCount.incrementAndGet());
        router.registerComponentPrefixHandler("ticket:close:", ignored -> specificPrefixCount.incrementAndGet());

        router.handleInteraction(interactionPayload(3, null, "ticket:close:42", "1", "token", null, null));

        assertEquals(0, broadPrefixCount.get());
        assertEquals(1, specificPrefixCount.get());
    }

    @Test
    void routesModalInteractionsUsingPrefixHandlers() throws Exception {
        AtomicReference<String> capturedCustomId = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerModalPrefixContextHandler("feedback:", interaction -> capturedCustomId.set(interaction.context().customId()));

        router.handleInteraction(interactionPayload(5, null, "feedback:long-form", "1", "token", null, null));

        assertEquals("feedback:long-form", capturedCustomId.get());
    }

    @Test
    void prefersExactComponentHandlerOverPrefixHandler() throws Exception {
        AtomicInteger exactCount = new AtomicInteger(0);
        AtomicInteger prefixCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerComponentPrefixHandler("ticket:", ignored -> prefixCount.incrementAndGet());
        router.registerComponentHandler("ticket:close:42", ignored -> exactCount.incrementAndGet());

        router.handleInteraction(interactionPayload(3, null, "ticket:close:42", "1", "token", null, null));

        assertEquals(1, exactCount.get());
        assertEquals(0, prefixCount.get());
    }

    @Test
    void duplicatePrefixHandlerRegistrationOverridesPreviousHandler() throws Exception {
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        AtomicInteger firstCount = new AtomicInteger(0);
        AtomicInteger secondCount = new AtomicInteger(0);

        router.registerModalPrefixHandler("feedback:", ignored -> firstCount.incrementAndGet());
        router.registerModalPrefixHandler("feedback:", ignored -> secondCount.incrementAndGet());

        router.handleInteraction(interactionPayload(5, null, "feedback:updated", "1", "token", null, null));

        assertEquals(0, firstCount.get());
        assertEquals(1, secondCount.get());
    }

    @Test
    void duplicateHandlerRegistrationOverridesPreviousHandler() throws Exception {
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        AtomicInteger firstCount = new AtomicInteger(0);
        AtomicInteger secondCount = new AtomicInteger(0);

        router.registerSlashHandler("ping", ignored -> firstCount.incrementAndGet());
        router.registerSlashHandler("ping", ignored -> secondCount.incrementAndGet());

        router.handleInteraction(interactionPayload(2, "ping", null, "1", "token", null, 1));

        assertEquals(0, firstCount.get());
        assertEquals(1, secondCount.get());
    }


    @Test
    void respondsEphemeralWhenSlashCommandHasNoRegisteredHandler() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });

        router.handleInteraction(interactionPayload(2, "unknown", null, "123", "abc", null, 1));

        assertEquals(4, responseType.get());
        assertEquals("This interaction is no longer available. Please try again.", responseData.get().get("content"));
        assertEquals(64, responseData.get().get("flags"));
    }

    @Test
    void respondsWithEmptyAutocompleteChoicesWhenHandlerMissing() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });

        router.handleInteraction(interactionPayload(4, "unknown", null, "123", "abc", "he", null));

        assertEquals(8, responseType.get());
        assertEquals(List.of(), responseData.get().get("choices"));
    }

    @Test
    void respondsEphemeralWhenHandlerThrowsRuntimeException() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });
        router.registerSlashHandler("explode", ignored -> {
            throw new IllegalStateException("boom");
        });

        router.handleInteraction(interactionPayload(2, "explode", null, "123", "abc", null, 1));

        assertEquals(4, responseType.get());
        assertEquals("Something went wrong while handling this interaction.", responseData.get().get("content"));
        assertEquals(64, responseData.get().get("flags"));
    }

    @Test
    void respondWithMessageRequiresIdAndToken() throws Exception {
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });

        JsonNode missingId = interactionPayload(2, "ping", null, "", "token", null, 1);
        JsonNode missingToken = interactionPayload(2, "ping", null, "id", "", null, 1);

        assertThrows(IllegalArgumentException.class, () -> router.respondWithMessage(missingId, "pong"));
        assertThrows(IllegalArgumentException.class, () -> router.respondWithMessage(missingToken, "pong"));
    }

    private static JsonNode interactionPayload(
            int type,
            String commandName,
            String customId,
            String id,
            String token,
            String optionValue,
            Integer commandType
    ) throws Exception {
        String data = switch (type) {
            case 2 -> {
                String typeField = commandType == null ? "" : "\"type\": %d, ".formatted(commandType);
                yield optionValue == null
                        ? "{%s\"name\": \"%s\"}".formatted(typeField, commandName)
                        : "{%s\"name\": \"%s\", \"options\": [{\"name\": \"text\", \"value\": \"%s\"}]}".formatted(typeField, commandName, optionValue);
            }
            case 4 -> optionValue == null
                    ? "{\"name\": \"%s\"}".formatted(commandName)
                    : "{\"name\": \"%s\", \"options\": [{\"name\": \"text\", \"value\": \"%s\"}]}".formatted(commandName, optionValue);
            case 3, 5 -> "{\"custom_id\": \"%s\"}".formatted(customId);
            default -> "{}";
        };

        String json = """
                {
                  "type": %d,
                  "id": "%s",
                  "token": "%s",
                  "data": %s
                }
                """.formatted(type, id, token, data);
        return MAPPER.readTree(json);
    }
}
