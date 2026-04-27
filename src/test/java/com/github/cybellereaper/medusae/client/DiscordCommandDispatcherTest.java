package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ComponentContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.SelectContext;
import com.github.cybellereaper.medusae.commands.core.response.InteractionReply;
import com.github.cybellereaper.medusae.commands.discord.adapter.DiscordCommandDispatcher;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscordCommandDispatcherTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void sendsFallbackEphemeralMessageWhenHandlerDoesNotRespond() throws Exception {
        CommandFramework framework = new CommandFramework();
        framework.registerCommands(new SilentCommand());
        DiscordCommandDispatcher dispatcher = new DiscordCommandDispatcher(framework);

        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> data = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        InteractionContext context = InteractionContext.from(interactionJson(), (id, token, type, payload) -> {
            calls.incrementAndGet();
            responseType.set(type);
            data.set(payload);
        });

        dispatcher.dispatch(interactionJson(), context);

        assertEquals(1, calls.get());
        assertEquals(4, responseType.get());
        assertEquals("Command completed without sending a response.", data.get().get("content"));
        assertEquals(64, data.get().get("flags"));
    }

    @Test
    void dispatchesGenericComponentByDiscordComponentType() throws Exception {
        CommandFramework framework = new CommandFramework();
        framework.registerModules(new ComponentModule());
        DiscordCommandDispatcher dispatcher = new DiscordCommandDispatcher(framework);

        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> data = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        InteractionContext context = InteractionContext.from(componentInteractionJson(), (id, token, type, payload) -> {
            calls.incrementAndGet();
            responseType.set(type);
            data.set(payload);
        });

        dispatcher.dispatchComponent(componentInteractionJson(), context);

        assertEquals(1, calls.get());
        assertEquals(7, responseType.get());
        assertEquals("Handled confirm button", data.get().get("content"));
    }

    @Test
    void dispatchesEntitySelectComponentsByDiscordComponentType() throws Exception {
        CommandFramework framework = new CommandFramework();
        framework.registerModules(new SelectComponentModule());
        DiscordCommandDispatcher dispatcher = new DiscordCommandDispatcher(framework);

        assertComponentDispatch(dispatcher, 5, "user_select", "Handled user_select");
        assertComponentDispatch(dispatcher, 6, "role_select", "Handled role_select");
        assertComponentDispatch(dispatcher, 7, "mention_select", "Handled mention_select");
        assertComponentDispatch(dispatcher, 8, "channel_select", "Handled channel_select");
    }

    @Command("silent")
    static final class SilentCommand {
        @Execute
        void root() {
            // Intentionally no response to verify fallback acknowledgement behavior.
        }
    }

    static final class ComponentModule {
        @ButtonHandler("confirm_button")
        InteractionReply close(ComponentContext ctx) {
            return InteractionReply.updateMessage()
                    .content("Handled confirm button")
                    .build();
        }
    }

    static final class SelectComponentModule {
        @UserSelectHandler("user_select")
        InteractionReply user(SelectContext ignored) {
            return InteractionReply.updateMessage().content("Handled user_select").build();
        }

        @RoleSelectHandler("role_select")
        InteractionReply role(SelectContext ignored) {
            return InteractionReply.updateMessage().content("Handled role_select").build();
        }

        @MentionableSelectHandler("mention_select")
        InteractionReply mention(SelectContext ignored) {
            return InteractionReply.updateMessage().content("Handled mention_select").build();
        }

        @ChannelSelectHandler("channel_select")
        InteractionReply channel(SelectContext ignored) {
            return InteractionReply.updateMessage().content("Handled channel_select").build();
        }
    }


    private static void assertComponentDispatch(DiscordCommandDispatcher dispatcher, int componentType, String customId, String expectedContent) throws Exception {
        AtomicReference<Integer> responseType = new AtomicReference<>();
        AtomicReference<Map<String, Object>> data = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();

        var interaction = componentInteractionJson(componentType, customId);
        InteractionContext context = InteractionContext.from(interaction, (id, token, type, payload) -> {
            calls.incrementAndGet();
            responseType.set(type);
            data.set(payload);
        });

        dispatcher.dispatchComponent(interaction, context);

        assertEquals(1, calls.get());
        assertEquals(7, responseType.get());
        assertEquals(expectedContent, data.get().get("content"));
    }

    private static DiscordInteractionPayload interactionJson() throws Exception {
        return MAPPER.readValue("""
                {
                  "id": "123",
                  "token": "abc",
                  "type": 2,
                  "data": {
                    "name": "silent",
                    "type": 1,
                    "options": []
                  }
                }
                """, DiscordInteractionPayload.class);
    }

    private static DiscordInteractionPayload componentInteractionJson() throws Exception {
        return componentInteractionJson(2, "confirm_button");
    }

    private static DiscordInteractionPayload componentInteractionJson(int componentType, String customId) throws Exception {
        return MAPPER.readValue("""
                {
                  "id": "124",
                  "token": "component-token",
                  "type": 3,
                  "data": {
                    "custom_id": "%s",
                    "component_type": %d
                  }
                }
                """.formatted(customId, componentType), DiscordInteractionPayload.class);
    }
}
