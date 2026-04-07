package com.github.cybellereaper.medusae.commands.core.registry;

import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.model.CommandDefinition;
import com.github.cybellereaper.medusae.commands.core.model.CommandHandler;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandRegistry {
    private final Map<String, CommandDefinition> byName = new ConcurrentHashMap<>();

    private static boolean sameRoute(String a, String b) {
        if (a == null || a.isBlank()) {
            return b == null || b.isBlank();
        }
        return a.equalsIgnoreCase(b == null ? "" : b);
    }

    private static String key(String name) {
        if (name == null || name.isBlank()) {
            throw new RegistrationException("command name must not be blank");
        }
        return name.trim().toLowerCase();
    }

    public void register(CommandDefinition definition) {
        String key = key(definition.name());
        CommandDefinition previous = byName.putIfAbsent(key, definition);
        if (previous != null) {
            throw new RegistrationException("Command already registered: " + definition.name());
        }
    }

    public Optional<CommandDefinition> find(String name) {
        return Optional.ofNullable(byName.get(key(name)));
    }

    public Optional<CommandHandler> findHandler(String name, String routeKey) {
        return find(name)
                .flatMap(def -> def.handlers().stream().filter(it -> sameRoute(it.routeKey(), routeKey)).findFirst());
    }

    public Collection<CommandDefinition> all() {
        return byName.values();
    }
}
