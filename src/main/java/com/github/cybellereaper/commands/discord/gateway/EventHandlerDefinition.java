package com.github.cybellereaper.commands.discord.gateway;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.commands.core.exception.RegistrationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public record EventHandlerDefinition(
        Object moduleInstance,
        Method method,
        String eventType,
        Class<?> payloadClass,
        int payloadParameterIndex,
        int clientParameterIndex
) {
    Consumer<Object> listener(DiscordClient client) {
        return payload -> invoke(payload, client);
    }

    private void invoke(Object payload, DiscordClient client) {
        Object[] arguments = new Object[method.getParameterCount()];
        arguments[payloadParameterIndex] = payload;
        if (clientParameterIndex >= 0) {
            arguments[clientParameterIndex] = client;
        }

        try {
            method.invoke(moduleInstance, arguments);
        } catch (IllegalAccessException e) {
            throw new RegistrationException("Unable to access gateway event handler method: " + method + ". Cause: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RegistrationException("Gateway event handler threw an exception: " + method + ". Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        }
    }
}
