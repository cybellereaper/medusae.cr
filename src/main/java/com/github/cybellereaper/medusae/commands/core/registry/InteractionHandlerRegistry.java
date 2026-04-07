package com.github.cybellereaper.medusae.commands.core.registry;

import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.interaction.route.ComponentRouteTemplate;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandler;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import com.github.cybellereaper.medusae.commands.core.model.InteractionRouteMatch;

import java.util.*;

public final class InteractionHandlerRegistry {
    private final Map<InteractionHandlerType, List<Entry>> handlersByType = new EnumMap<>(InteractionHandlerType.class);

    public void register(InteractionHandler handler) {
        Objects.requireNonNull(handler, "handler");
        Entry entry = new Entry(handler, ComponentRouteTemplate.compile(handler.route()));
        List<Entry> entries = handlersByType.computeIfAbsent(handler.type(), ignored -> new ArrayList<>());
        for (Entry existing : entries) {
            if (entry.template.conflictsWith(existing.template)) {
                throw new RegistrationException("Conflicting interaction routes for " + handler.type() + ": '" + handler.route() + "' and '" + existing.handler.route() + "'");
            }
        }
        entries.add(entry);
    }

    public Optional<ResolvedInteractionHandler> find(InteractionHandlerType type, String customId) {
        for (Entry entry : handlersByType.getOrDefault(type, List.of())) {
            Optional<InteractionRouteMatch> matched = entry.template.match(customId);
            if (matched.isPresent()) {
                return Optional.of(new ResolvedInteractionHandler(entry.handler, matched.get()));
            }
        }
        return Optional.empty();
    }

    public List<InteractionHandler> all() {
        return handlersByType.values().stream().flatMap(List::stream).map(entry -> entry.handler).toList();
    }

    private record Entry(InteractionHandler handler, ComponentRouteTemplate template) {
    }

    public record ResolvedInteractionHandler(InteractionHandler handler, InteractionRouteMatch routeMatch) {
    }
}
