package com.github.cybellereaper.medusae.commands.core.model;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public record InteractionHandler(
        Object instance,
        Method method,
        InteractionHandlerType type,
        String route,
        Set<InteractionSource> allowedSources,
        boolean ephemeralDefault,
        boolean deferReply,
        boolean deferUpdate,
        List<String> checks,
        List<String> requiredUserPermissions,
        List<String> requiredBotPermissions,
        CooldownSpec cooldown,
        List<InteractionParameter> parameters
) {
}
