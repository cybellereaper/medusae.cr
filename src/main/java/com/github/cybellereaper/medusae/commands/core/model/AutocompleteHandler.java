package com.github.cybellereaper.medusae.commands.core.model;

import java.lang.reflect.Method;

public record AutocompleteHandler(
        Object instance,
        Method method,
        String routeKey,
        String optionName,
        String providerId
) {
}
