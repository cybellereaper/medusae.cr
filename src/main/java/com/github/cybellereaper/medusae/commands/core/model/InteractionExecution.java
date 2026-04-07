package com.github.cybellereaper.medusae.commands.core.model;

import java.util.Map;
import java.util.Optional;

public record InteractionExecution(
        InteractionHandlerType type,
        String customId,
        Map<String, String> modalFields,
        Object rawInteraction,
        ResolvedEntities resolved,
        boolean dm,
        String guildId,
        String userId,
        java.util.Set<String> userPermissions,
        java.util.Set<String> botPermissions,
        String statePayload
) {
    public InteractionExecution {
        modalFields = modalFields == null ? Map.of() : Map.copyOf(modalFields);
        resolved = resolved == null ? ResolvedEntities.empty() : resolved;
        userPermissions = userPermissions == null ? java.util.Set.of() : java.util.Set.copyOf(userPermissions);
        botPermissions = botPermissions == null ? java.util.Set.of() : java.util.Set.copyOf(botPermissions);
    }

    public Optional<String> field(String name) {
        return Optional.ofNullable(modalFields.get(name));
    }
}
