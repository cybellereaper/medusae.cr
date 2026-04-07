package com.github.cybellereaper.medusae.commands.discord.gateway;

import com.github.cybellereaper.medusae.client.DiscordClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AnnotatedGatewayEventBinder {
    private final EventModuleParser parser;

    public AnnotatedGatewayEventBinder() {
        this(new EventModuleParser());
    }

    AnnotatedGatewayEventBinder(EventModuleParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public List<EventModuleDefinition> bind(DiscordClient client, Object... modules) {
        Objects.requireNonNull(client, "client");
        return bind(new DiscordClientSubscriber(client), modules);
    }

    List<EventModuleDefinition> bind(GatewayEventSubscriber subscriber, Object... modules) {
        Objects.requireNonNull(subscriber, "subscriber");
        Objects.requireNonNull(modules, "modules");

        List<EventModuleDefinition> moduleDefinitions = new ArrayList<>();
        for (Object module : modules) {
            EventModuleDefinition moduleDefinition = parser.parse(module);
            moduleDefinitions.add(moduleDefinition);
            moduleDefinition.handlers().forEach(handler -> subscriber.on(
                    handler.eventType(),
                    handler.payloadClass(),
                    handler.listener(subscriber.client())
            ));
        }
        return List.copyOf(moduleDefinitions);
    }

    interface GatewayEventSubscriber {
        void on(String eventType, Class<?> payloadClass, java.util.function.Consumer<Object> listener);

        DiscordClient client();
    }

    private record DiscordClientSubscriber(DiscordClient client) implements GatewayEventSubscriber {
        private DiscordClientSubscriber {
            Objects.requireNonNull(client, "client");
        }

        @SuppressWarnings("unchecked")
        @Override
        public void on(String eventType, Class<?> payloadClass, java.util.function.Consumer<Object> listener) {
            client.on(eventType, (Class<Object>) payloadClass, listener);
        }
    }
}
