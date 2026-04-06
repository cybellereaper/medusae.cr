package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.commands.core.annotation.Command;
import com.github.cybellereaper.commands.core.annotation.Execute;
import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.discord.adapter.DiscordCommandDispatcher;
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

    @Command("silent")
    static final class SilentCommand {
        @Execute
        void root() {
            // Intentionally no response to verify fallback acknowledgement behavior.
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
}
