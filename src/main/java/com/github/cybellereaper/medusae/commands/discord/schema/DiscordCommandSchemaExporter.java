package com.github.cybellereaper.medusae.commands.discord.schema;

import com.github.cybellereaper.medusae.client.SlashCommandDefinition;
import com.github.cybellereaper.medusae.client.SlashCommandOptionChoice;
import com.github.cybellereaper.medusae.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.medusae.commands.core.model.CommandDefinition;
import com.github.cybellereaper.medusae.commands.core.model.CommandHandler;
import com.github.cybellereaper.medusae.commands.core.model.CommandOptionChoice;
import com.github.cybellereaper.medusae.commands.core.model.CommandParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DiscordCommandSchemaExporter {

    public List<SlashCommandDefinition> export(Iterable<CommandDefinition> definitions) {
        List<SlashCommandDefinition> result = new ArrayList<>();
        for (CommandDefinition definition : definitions) {
            result.add(exportDefinition(definition));
        }
        return result;
    }

    public SlashCommandDefinition exportDefinition(CommandDefinition definition) {
        return switch (definition.type()) {
            case USER_CONTEXT -> SlashCommandDefinition.userContextMenu(definition.name());
            case MESSAGE_CONTEXT -> SlashCommandDefinition.messageContextMenu(definition.name());
            case CHAT_INPUT -> exportSlash(definition);
        };
    }

    private SlashCommandDefinition exportSlash(CommandDefinition definition) {
        List<SlashCommandOptionDefinition> options = new ArrayList<>();
        List<CommandHandler> routedHandlers = definition.handlers().stream().filter(h -> h.subcommand() != null).toList();

        if (!routedHandlers.isEmpty()) {
            Map<String, List<CommandHandler>> grouped = routedHandlers.stream()
                    .filter(it -> it.subcommandGroup() != null)
                    .collect(Collectors.groupingBy(CommandHandler::subcommandGroup));
            grouped.forEach((groupName, handlers) -> options.add(SlashCommandOptionDefinition.subcommandGroup(
                    groupName,
                    "Subcommand group",
                    handlers.stream().map(this::toSubcommand).toList()
            )));
            routedHandlers.stream().filter(it -> it.subcommandGroup() == null).forEach(handler -> options.add(toSubcommand(handler)));
        }

        definition.handlers().stream().filter(h -> h.subcommand() == null).findFirst()
                .ifPresent(root -> root.parameters().stream().filter(this::isSchemaOption).map(this::toOption).forEach(options::add));

        return new SlashCommandDefinition(definition.name(), definition.description(), options);
    }

    private SlashCommandOptionDefinition toSubcommand(CommandHandler handler) {
        List<SlashCommandOptionDefinition> options = handler.parameters().stream().filter(this::isSchemaOption).map(this::toOption).toList();
        return SlashCommandOptionDefinition.subcommand(handler.subcommand(), handler.description(), options);
    }

    private boolean isSchemaOption(CommandParameter parameter) {
        return switch (parameter.kind()) {
            case OPTION, TARGET_USER, TARGET_MEMBER, TARGET_CHANNEL, TARGET_ROLE, TARGET_ATTACHMENT -> true;
            default -> false;
        };
    }

    private SlashCommandOptionDefinition toOption(CommandParameter parameter) {
        return new SlashCommandOptionDefinition(
                mapOptionType(parameter),
                parameter.optionName(),
                parameter.description(),
                parameter.required(),
                parameter.autocompleteId() != null,
                List.of(),
                toChoices(parameter),
                parameter.minValue(),
                parameter.maxValue(),
                parameter.minLength(),
                parameter.maxLength(),
                parameter.channelTypes(),
                parameter.nameLocalizations(),
                parameter.descriptionLocalizations()
        );
    }

    private List<SlashCommandOptionChoice> toChoices(CommandParameter parameter) {
        return parameter.choices().stream().map(choice -> new SlashCommandOptionChoice(choice.name(), toChoiceValue(parameter, choice))).toList();
    }

    private Object toChoiceValue(CommandParameter parameter, CommandOptionChoice choice) {
        Class<?> optionType = parameter.optionType();
        if (optionType == int.class || optionType == Integer.class) {
            return Integer.parseInt(choice.value());
        }
        if (optionType == long.class || optionType == Long.class) {
            return Long.parseLong(choice.value());
        }
        if (optionType == double.class || optionType == Double.class) {
            return Double.parseDouble(choice.value());
        }
        return choice.value();
    }

    private int mapOptionType(CommandParameter parameter) {
        Class<?> type = parameter.optionType();
        if (type == String.class || type.isEnum()) {
            return SlashCommandOptionDefinition.STRING;
        }
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
            return SlashCommandOptionDefinition.INTEGER;
        }
        if (type == boolean.class || type == Boolean.class) {
            return SlashCommandOptionDefinition.BOOLEAN;
        }
        if (type == double.class || type == Double.class) {
            return SlashCommandOptionDefinition.NUMBER;
        }
        return switch (parameter.kind()) {
            case TARGET_USER, TARGET_MEMBER -> SlashCommandOptionDefinition.USER;
            case TARGET_CHANNEL -> SlashCommandOptionDefinition.CHANNEL;
            case TARGET_ROLE -> SlashCommandOptionDefinition.ROLE;
            case TARGET_ATTACHMENT -> SlashCommandOptionDefinition.ATTACHMENT;
            default -> SlashCommandOptionDefinition.STRING;
        };
    }
}
