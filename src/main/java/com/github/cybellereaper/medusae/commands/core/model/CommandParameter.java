package com.github.cybellereaper.medusae.commands.core.model;

import java.lang.reflect.Parameter;

public record CommandParameter(
        int index,
        Parameter reflectedParameter,
        Class<?> optionType,
        String optionName,
        String description,
        ParameterKind kind,
        boolean required,
        String defaultValue,
        String autocompleteId,
        boolean wrappedOptional
) {
}
