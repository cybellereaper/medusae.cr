package com.github.cybellereaper.medusae.commands.core.model;

import java.lang.reflect.Method;
import java.util.List;

public record CommandHandler(
        Object instance,
        Method method,
        String subcommandGroup,
        String subcommand,
        String description,
        boolean hidden,
        List<String> checks,
        List<String> requiredUserPermissions,
        List<String> requiredBotPermissions,
        CooldownSpec cooldown,
        List<CommandParameter> parameters,
        String routeKey
) {
}
