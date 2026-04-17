package com.github.cybellereaper.medusae.client;

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
    void routesComponentAndModalToGlobalHandlersWhenCustomIdIsNotRegistered() throws Exception {
        AtomicInteger componentCount = new AtomicInteger(0);
        AtomicInteger modalCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerGlobalComponentContextHandler(context -> componentCount.incrementAndGet());
        router.registerGlobalModalContextHandler(context -> modalCount.incrementAndGet());

        router.handleInteraction(interactionPayload(3, null, "ticket:close:42|sig", "1", "token", null, null));
        router.handleInteraction(interactionPayload(5, null, "ticket:create|sig", "2", "token", null, null));

        assertEquals(1, componentCount.get());
        assertEquals(1, modalCount.get());
    }

    @Test
    void prefersExactComponentHandlerOverGlobalHandler() throws Exception {
        AtomicInteger exactCount = new AtomicInteger(0);
        AtomicInteger globalCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerComponentHandler("confirm_button", ignored -> exactCount.incrementAndGet());
        router.registerGlobalComponentContextHandler(context -> globalCount.incrementAndGet());

        router.handleInteraction(interactionPayload(3, null, "confirm_button", "1", "token", null, null));

        assertEquals(1, exactCount.get());
        assertEquals(0, globalCount.get());
    }


    @Test
    void routesUnknownApplicationCommandTypeToSlashHandlers() throws Exception {
        AtomicInteger slashCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerSlashHandler("fallback", ignored -> slashCount.incrementAndGet());

        router.handleInteraction(interactionPayload(2, "fallback", null, "77", "token-value", null, 99));

        assertEquals(1, slashCount.get());
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
    void ignoresNullAndUnknownInteractionTypes() {
        AtomicInteger invocationCount = new AtomicInteger(0);

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> invocationCount.incrementAndGet());

        router.handleInteraction(null);
        assertDoesNotThrow(() -> router.handleInteraction(MAPPER.createObjectNode().put("type", 999)));
        assertEquals(0, invocationCount.get());
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
    void deferMethodsUseExpectedResponseTypes() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> responseType.set(type));
        JsonNode interaction = interactionPayload(2, "ping", null, "123", "abc", null, 1);

        router.deferMessage(interaction);
        assertEquals(5, responseType.get());

        router.deferUpdate(interaction);
        assertEquals(6, responseType.get());
    }

    @Test
    void respondWithUpdatedMessageUsesUpdateMessageResponseType() throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });

        router.respondWithUpdatedMessage(
                interactionPayload(3, null, "confirm_button", "123", "abc", null, null),
                DiscordMessage.ofContent("updated content")
        );

        assertEquals(7, responseType.get());
        assertEquals("updated content", responseData.get().get("content"));
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
    void rejectsDuplicateHandlerRegistration() {
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });
        router.registerSlashHandler("ping", ignored -> {
        });

        assertThrows(IllegalArgumentException.class, () -> router.registerSlashHandler("ping", ignored -> {
        }));
    }


    @Test
    void rejectsBlankHandlerKey() {
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });

        assertThrows(IllegalArgumentException.class, () -> router.registerSlashHandler(" ", ignored -> {
        }));
    }

    @Test
    void trimsHandlerKeyDuringRegistrationAndDispatch() throws Exception {
        AtomicInteger invocationCount = new AtomicInteger(0);
        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
        });

        router.registerSlashHandler(" ping ", ignored -> invocationCount.incrementAndGet());

        router.handleInteraction(interactionPayload(2, "ping", null, "42", "token-value", null, 1));
        assertEquals(1, invocationCount.get());
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

    @Test
    void supportsContextBasedHandlersAndResponses() throws Exception {
        AtomicReference<String> optionValue = new AtomicReference<>();
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> responseData = new AtomicReference<>();

        SlashCommandRouter router = new SlashCommandRouter((id, token, type, data) -> {
            responseType.set(type);
            responseData.set(data);
        });

        router.registerSlashContextHandler("echo", context -> {
            optionValue.set(context.optionString("text"));
            context.respondEphemeral("ok");
        });

        router.handleInteraction(interactionPayload(2, "echo", null, "id-1", "token-1", "hello", 1));

        assertEquals("hello", optionValue.get());
        assertEquals(4, responseType.get());
        assertEquals(64, responseData.get().get("flags"));
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
