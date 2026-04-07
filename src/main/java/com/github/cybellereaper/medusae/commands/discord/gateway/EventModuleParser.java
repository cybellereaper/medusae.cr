package com.github.cybellereaper.medusae.commands.discord.gateway;

import com.github.cybellereaper.medusae.client.DiscordClient;
import com.github.cybellereaper.medusae.commands.core.annotation.EventModule;
import com.github.cybellereaper.medusae.commands.core.annotation.OnGatewayEvent;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EventModuleParser {
    private static String normalizeEventType(String value, Method method) {
        String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || normalized.isBlank()) {
            throw new RegistrationException("@OnGatewayEvent value must not be blank: " + method);
        }
        return normalized;
    }

    public EventModuleDefinition parse(Object moduleInstance) {
        Objects.requireNonNull(moduleInstance, "moduleInstance");
        Class<?> moduleType = moduleInstance.getClass();

        if (!moduleType.isAnnotationPresent(EventModule.class)) {
            throw new RegistrationException("Gateway event modules must be annotated with @EventModule: " + moduleType.getName());
        }

        List<EventHandlerDefinition> handlers = new ArrayList<>();
        for (Method method : moduleType.getDeclaredMethods()) {
            OnGatewayEvent annotation = method.getAnnotation(OnGatewayEvent.class);
            if (annotation == null) {
                continue;
            }
            handlers.add(parseHandler(moduleInstance, method, annotation));
        }

        if (handlers.isEmpty()) {
            throw new RegistrationException("@EventModule contains no @OnGatewayEvent handlers: " + moduleType.getName());
        }

        return new EventModuleDefinition(moduleInstance, List.copyOf(handlers));
    }

    private EventHandlerDefinition parseHandler(Object moduleInstance, Method method, OnGatewayEvent annotation) {
        if (Modifier.isPrivate(method.getModifiers())) {
            throw new RegistrationException("@OnGatewayEvent handler methods must not be private: " + method);
        }
        method.setAccessible(true);

        String eventType = normalizeEventType(annotation.value(), method);
        Class<?> payloadClass = annotation.payload();

        int payloadIndex = -1;
        int clientIndex = -1;

        Parameter[] parameters = method.getParameters();
        for (int index = 0; index < parameters.length; index++) {
            Class<?> parameterType = parameters[index].getType();
            if (parameterType.isAssignableFrom(payloadClass)) {
                if (payloadIndex >= 0) {
                    throw new RegistrationException("@OnGatewayEvent methods must declare exactly one payload parameter compatible with %s: %s"
                            .formatted(payloadClass.getName(), method));
                }
                payloadIndex = index;
                continue;
            }

            if (parameterType == DiscordClient.class) {
                if (clientIndex >= 0) {
                    throw new RegistrationException("@OnGatewayEvent methods may declare DiscordClient at most once: " + method);
                }
                clientIndex = index;
                continue;
            }

            throw new RegistrationException("Unsupported @OnGatewayEvent parameter type %s on method %s. Allowed parameters are payload (%s-compatible) and DiscordClient."
                    .formatted(parameterType.getName(), method, payloadClass.getName()));
        }

        if (payloadIndex < 0) {
            throw new RegistrationException("@OnGatewayEvent methods must declare exactly one payload parameter compatible with %s: %s"
                    .formatted(payloadClass.getName(), method));
        }

        return new EventHandlerDefinition(moduleInstance, method, eventType, payloadClass, payloadIndex, clientIndex);
    }
}
