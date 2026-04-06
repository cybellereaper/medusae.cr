package com.github.cybellereaper.commands.core.resolve;

import com.github.cybellereaper.commands.core.execute.CommandContext;
import com.github.cybellereaper.commands.core.model.CommandParameter;

@FunctionalInterface
public interface ParameterResolver<T> {
    T resolve(CommandContext context, CommandParameter parameter);
}
