package com.github.cybellereaper.commands.core.autocomplete;

import com.github.cybellereaper.commands.core.execute.CommandContext;

import java.util.List;

@FunctionalInterface
public interface AutocompleteProvider {
    List<String> complete(CommandContext context, String currentValue);
}
