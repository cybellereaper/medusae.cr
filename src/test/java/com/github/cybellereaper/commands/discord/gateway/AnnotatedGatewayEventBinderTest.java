package com.github.cybellereaper.commands.discord.gateway;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.commands.core.annotation.EventModule;
import com.github.cybellereaper.commands.core.annotation.OnGatewayEvent;
import com.github.cybellereaper.gateway.events.MessageCreateEvent;
import com.github.cybellereaper.gateway.events.ReadyEvent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class AnnotatedGatewayEventBinderTest {

    @Test
    void bindsAndDispatchesAnnotatedHandlers() {
        AnnotatedGatewayEventBinder binder = new AnnotatedGatewayEventBinder();
        RecordingSubscriber subscriber = new RecordingSubscriber(null);
        TestEventModule module = new TestEventModule();

        binder.bind(subscriber, module);

        ReadyEvent readyEvent = new ReadyEvent("session", "wss://gateway.discord.gg");
        MessageCreateEvent messageEvent = new MessageCreateEvent(
                "1",
                "100",
                "200",
                "!ping",
                new MessageCreateEvent.Author("3", "tester", "0001")
        );

        subscriber.dispatch("READY", readyEvent);
        subscriber.dispatch("MESSAGE_CREATE", messageEvent);

        assertEquals("session", module.lastSessionId);
        assertEquals("!ping", module.lastMessageContent);
    }

    @Test
    void injectsDiscordClientWhenRequested() {
        AnnotatedGatewayEventBinder binder = new AnnotatedGatewayEventBinder();
        DiscordClient client = null;
        RecordingSubscriber subscriber = new RecordingSubscriber(client);
        ClientAwareEventModule module = new ClientAwareEventModule();

        binder.bind(subscriber, module);
        subscriber.dispatch("READY", new ReadyEvent("session", "resume"));

        assertTrue(module.clientInjected);
    }

    @EventModule
    static final class TestEventModule {
        private String lastSessionId;
        private String lastMessageContent;

        @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
        void onReady(ReadyEvent event) {
            lastSessionId = event.sessionId();
        }

        @OnGatewayEvent(value = "MESSAGE_CREATE", payload = MessageCreateEvent.class)
        void onMessage(MessageCreateEvent event) {
            lastMessageContent = event.content();
        }
    }

    @EventModule
    static final class ClientAwareEventModule {
        private boolean clientInjected;

        @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
        void onReady(ReadyEvent event, DiscordClient client) {
            clientInjected = true;
        }
    }

    static final class RecordingSubscriber implements AnnotatedGatewayEventBinder.GatewayEventSubscriber {
        private final DiscordClient client;
        private final Map<String, Consumer<Object>> listeners = new HashMap<>();

        RecordingSubscriber(DiscordClient client) {
            this.client = client;
        }

        @Override
        public void on(String eventType, Class<?> payloadClass, Consumer<Object> listener) {
            listeners.put(eventType + "::" + payloadClass.getName(), listener);
        }

        @Override
        public DiscordClient client() {
            return client;
        }

        void dispatch(String eventType, Object payload) {
            Consumer<Object> listener = listeners.get(eventType + "::" + payload.getClass().getName());
            assertNotNull(listener, "listener not registered for " + eventType + " and " + payload.getClass().getName());
            listener.accept(payload);
        }
    }
}
