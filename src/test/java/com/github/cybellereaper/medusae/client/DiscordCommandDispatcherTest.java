package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.commands.core.annotation.Command;
import com.github.cybellereaper.medusae.commands.core.annotation.Execute;
import com.github.cybellereaper.medusae.commands.core.annotation.ButtonHandler;
import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ComponentContext;
import com.github.cybellereaper.medusae.commands.core.response.InteractionReply;
import com.github.cybellereaper.medusae.commands.discord.adapter.DiscordCommandDispatcher;
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

    private static com.fasterxml.jackson.databind.JsonNode interactionJson() throws Exception {
        return MAPPER.readTree("""
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
                """);
    }

    private static com.fasterxml.jackson.databind.JsonNode componentInteractionJson() throws Exception {
        return MAPPER.readTree("""
                {
                  "id": "124",
                  "token": "component-token",
                  "type": 3,
                  "data": {
                    "custom_id": "confirm_button",
                    "component_type": 2
                  }
                }
                """);
    }
}
