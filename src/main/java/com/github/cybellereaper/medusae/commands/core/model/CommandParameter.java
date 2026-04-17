package com.github.cybellereaper.medusae.commands.core.model;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

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
        boolean wrappedOptional,
        List<CommandOptionChoice> choices,
        Double minValue,
        Double maxValue,
        Integer minLength,
        Integer maxLength,
        List<Integer> channelTypes,
        Map<String, String> nameLocalizations,
        Map<String, String> descriptionLocalizations
) {
}
