package com.github.cybellereaper.commands.core.check;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CheckRegistry {
    private final Map<String, CommandCheck> checks = new ConcurrentHashMap<>();

    public void register(String id, CommandCheck check) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(check, "check");
        checks.put(normalizeId(id), check);
    }

    public Optional<CommandCheck> find(String id) {
        return Optional.ofNullable(checks.get(normalizeId(id)));
    }

    private static String normalizeId(String id) {
        String normalized = id.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("check id must not be blank");
        }
        return normalized;
    }
}
