package com.github.cybellereaper.medusae.commands.core.help;

import com.github.cybellereaper.medusae.commands.core.model.CommandDefinition;
import com.github.cybellereaper.medusae.commands.core.model.CommandHandler;
import com.github.cybellereaper.medusae.commands.core.model.CommandParameter;

import java.util.List;

public final class CommandIntrospector {
    public List<String> summarize(CommandDefinition definition) {
        return definition.handlers().stream().map(handler -> {
            String usage = usage(definition, handler);
            return usage + " - " + handler.description();
        }).toList();
    }

    public String usage(CommandDefinition definition, CommandHandler handler) {
        StringBuilder usage = new StringBuilder("/").append(definition.name());
        if (handler.subcommandGroup() != null) {
            usage.append(' ').append(handler.subcommandGroup());
        }
        if (handler.subcommand() != null) {
            usage.append(' ').append(handler.subcommand());
        }
        for (CommandParameter parameter : handler.parameters()) {
            if (parameter.kind().name().equals("OPTION") || parameter.kind().name().equals("CUSTOM")) {
                usage.append(' ').append(parameter.required() ? '<' : '[').append(parameter.optionName())
                        .append(parameter.required() ? '>' : ']');
            }
        }
        return usage.toString();
    }
}
