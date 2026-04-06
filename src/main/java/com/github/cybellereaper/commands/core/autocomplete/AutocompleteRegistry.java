package com.github.cybellereaper.commands.core.autocomplete;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AutocompleteRegistry {
    private final Map<String, AutocompleteProvider> providers = new ConcurrentHashMap<>();

    public void register(String id, AutocompleteProvider provider) {
        Objects.requireNonNull(provider, "provider");
        providers.put(normalize(id), provider);
    }

    public Optional<AutocompleteProvider> find(String id) {
        return Optional.ofNullable(providers.get(normalize(id)));
    }

    private static String normalize(String id) {
        Objects.requireNonNull(id, "id");
        String normalized = id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("autocomplete id must not be blank");
        }
        return normalized;
    }
}
