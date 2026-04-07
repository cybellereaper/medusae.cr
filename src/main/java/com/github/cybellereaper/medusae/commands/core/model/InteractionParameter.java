package com.github.cybellereaper.medusae.commands.core.model;

import java.lang.reflect.Parameter;

public record InteractionParameter(
        int index,
        Parameter parameter,
        Class<?> type,
        InteractionParameterKind kind,
        String key,
        boolean required,
        boolean wrappedOptional
) {
}
