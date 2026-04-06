package com.github.cybellereaper.commands.core.resolve;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ResolverRegistry {
    private final Map<Class<?>, ParameterResolver<?>> resolvers = new ConcurrentHashMap<>();

    public <T> void register(Class<T> type, ParameterResolver<? extends T> resolver) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(resolver, "resolver");
        resolvers.put(type, resolver);
    }

    public Optional<ParameterResolver<?>> find(Class<?> type) {
        return Optional.ofNullable(resolvers.get(type));
    }
}
