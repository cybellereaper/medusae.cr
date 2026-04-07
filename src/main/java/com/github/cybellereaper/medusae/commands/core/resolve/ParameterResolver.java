package com.github.cybellereaper.medusae.commands.core.resolve;

import com.github.cybellereaper.medusae.commands.core.execute.CommandContext;
import com.github.cybellereaper.medusae.commands.core.model.CommandParameter;

@FunctionalInterface
public interface ParameterResolver<T> {
    T resolve(CommandContext context, CommandParameter parameter);
}
