package com.github.cybellereaper.commands.discord.gateway;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.commands.core.annotation.EventModule;
import com.github.cybellereaper.commands.core.annotation.OnGatewayEvent;
import com.github.cybellereaper.commands.core.exception.RegistrationException;
import com.github.cybellereaper.gateway.events.ReadyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventModuleParserTest {
    private final EventModuleParser parser = new EventModuleParser();

    @Test
    void parsesAnnotatedEventModule() {
        EventModuleDefinition definition = parser.parse(new ValidModule());

        assertEquals(1, definition.handlers().size());
        EventHandlerDefinition handler = definition.handlers().getFirst();
        assertEquals("READY", handler.eventType());
        assertEquals(ReadyEvent.class, handler.payloadClass());
    }

    @Test
    void rejectsModuleWithoutEventModuleAnnotation() {
        RegistrationException exception = assertThrows(RegistrationException.class, () -> parser.parse(new MissingEventModuleAnnotation()));
        assertTrue(exception.getMessage().contains("@EventModule"));
    }

    @Test
    void rejectsPrivateHandlerMethod() {
        RegistrationException exception = assertThrows(RegistrationException.class, () -> parser.parse(new PrivateHandlerModule()));
        assertTrue(exception.getMessage().contains("must not be private"));
    }

    @Test
    void rejectsMissingPayloadParameter() {
        RegistrationException exception = assertThrows(RegistrationException.class, () -> parser.parse(new MissingPayloadModule()));
        assertTrue(exception.getMessage().contains("exactly one payload"));
    }

    @Test
    void rejectsDuplicateClientParameters() {
        RegistrationException exception = assertThrows(RegistrationException.class, () -> parser.parse(new DuplicateClientModule()));
        assertTrue(exception.getMessage().contains("DiscordClient at most once"));
    }

    @EventModule
    static final class ValidModule {
        @OnGatewayEvent(value = "ready", payload = ReadyEvent.class)
        void onReady(ReadyEvent event, DiscordClient client) {
        }
    }

    static final class MissingEventModuleAnnotation {
        @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
        void onReady(ReadyEvent event) {
        }
    }

    @EventModule
    static final class PrivateHandlerModule {
        @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
        private void onReady(ReadyEvent event) {
        }
    }

    @EventModule
    static final class MissingPayloadModule {
        @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
        void onReady(DiscordClient client) {
        }
    }

    @EventModule
    static final class DuplicateClientModule {
        @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
        void onReady(ReadyEvent event, DiscordClient clientOne, DiscordClient clientTwo) {
        }
    }
}
