package com.github.cybellereaper.medusae.commands.discord.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.*;
import com.github.cybellereaper.medusae.commands.core.model.*;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayloadReader;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordOptionType;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordOptionValue;

import java.util.*;

public final class DiscordInteractionMapper {
    private final DiscordInteractionPayloadReader payloadReader;

    public DiscordInteractionMapper() {
        this(new DiscordInteractionPayloadReader(new ObjectMapper()));
    }

    DiscordInteractionMapper(DiscordInteractionPayloadReader payloadReader) {
        this.payloadReader = payloadReader;
    }

    private static CommandType mapCommandType(int discordType) {
        return switch (discordType) {
            case 2 -> CommandType.USER_CONTEXT;
            case 3 -> CommandType.MESSAGE_CONTEXT;
            default -> CommandType.CHAT_INPUT;
        };
    }

    private static String focusedOption(List<DiscordInteractionPayload.Option> options) {
        if (options == null) {
            return null;
        }
        for (DiscordInteractionPayload.Option option : options) {
            if (Boolean.TRUE.equals(option.focused())) {
                return textOrNull(option.name());
            }
            String nested = focusedOption(option.options());
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static ParsedData parseOptions(List<DiscordInteractionPayload.Option> nodes, ResolvedEntities globalResolved, ParsedData parsedData) {
        if (nodes == null) {
            return parsedData;
        }

        for (DiscordInteractionPayload.Option option : nodes) {
            int rawType = orDefault(option.type(), 0);
            DiscordOptionType type = toOptionType(rawType);
            String name = textOrNull(option.name());
            if (name == null || type == null) {
                continue;
            }

            switch (type) {
                case SUB_COMMAND -> {
                    parsedData.subcommand = name;
                    parseOptions(option.options(), globalResolved, parsedData);
                }
                case SUB_COMMAND_GROUP -> {
                    parsedData.group = name;
                    var children = option.options();
                    if (children != null && !children.isEmpty()) {
                        DiscordInteractionPayload.Option subcommandNode = children.get(0);
                        parsedData.subcommand = textOrNull(subcommandNode.name());
                        parseOptions(subcommandNode.options(), globalResolved, parsedData);
                    }
                }
                case STRING, INTEGER, BOOLEAN, USER, CHANNEL, ROLE, MENTIONABLE, NUMBER, ATTACHMENT -> {
                    Object value = parseOptionValue(type, option.value());
                    parsedData.options.put(name, new CommandOptionValue(value, rawType));
                    ResolvedEntities resolvedSource = option.resolved() == null ? globalResolved : toResolvedEntities(option.resolved());
                    parsedData.collectResolvedEntities(name, type, value, resolvedSource);
                }
            }
        }

        return parsedData;
    }

    private static DiscordOptionType toOptionType(int rawType) {
        try {
            return DiscordOptionType.fromCode(rawType);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Object parseOptionValue(DiscordOptionType type, DiscordOptionValue rawValue) {
        if (rawValue == null) {
            return null;
        }
        Object value = rawValue.value();
        return switch (type) {
            case STRING -> value == null ? null : value.toString();
            case INTEGER -> parseLong(value);
            case BOOLEAN -> parseBoolean(value);
            case USER, CHANNEL, ROLE, MENTIONABLE, ATTACHMENT -> parseEntityId(value);
            case NUMBER -> parseDouble(value);
            case SUB_COMMAND, SUB_COMMAND_GROUP ->
                    throw new IllegalStateException("Subcommand options do not expose scalar values");
        };
    }

    private static String parseEntityId(Object rawValue) {
        if (rawValue instanceof String value) {
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (rawValue instanceof Number number) {
            return Long.toString(number.longValue());
        }
        return null;
    }

    private static Long parseLong(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        if (rawValue instanceof String value) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double parseDouble(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        if (rawValue instanceof String value) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean parseBoolean(Object rawValue) {
        if (rawValue instanceof Boolean value) {
            return value;
        }
        if (rawValue instanceof String value) {
            if ("true".equalsIgnoreCase(value.trim())) {
                return true;
            }
            if ("false".equalsIgnoreCase(value.trim())) {
                return false;
            }
        }
        return null;
    }

    private static String textOrNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static String extractStatePayload(String customId) {
        if (customId == null) return null;
        int index = customId.indexOf('|');
        if (index < 0 || index + 1 >= customId.length()) {
            return null;
        }
        return customId.substring(index + 1);
    }

    private static int orDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private static String stringOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private static ResolvedEntities toResolvedEntities(DiscordInteractionPayload.Resolved resolved) {
        if (resolved == null) {
            return ResolvedEntities.empty();
        }
        Map<String, ResolvedUser> users = copyResolved(resolved.users());
        return new ResolvedEntities(
                users,
                convertMembers(resolved.members(), users),
                copyResolved(resolved.roles()),
                copyResolved(resolved.channels()),
                copyResolved(resolved.messages()),
                copyResolved(resolved.attachments())
        );
    }

    private static <T> Map<String, T> copyResolved(Map<String, T> values) {
        return values == null || values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    public CommandInteraction toCoreInteraction(DiscordInteractionPayload interactionPayload, InteractionContext context) {
        Objects.requireNonNull(interactionPayload, "interaction");
        Objects.requireNonNull(context, "context");
        var data = interactionPayload.data();
        ResolvedEntities resolvedEntities = toResolvedEntities(data == null ? null : data.resolved());
        CommandType commandType = mapCommandType(orDefault(data == null ? null : data.type(), 1));

        ParsedData parsed = parseOptions(data == null ? null : data.options(), resolvedEntities, new ParsedData());

        String targetId = textOrNull(data == null ? null : data.targetId());
        ResolvedUser targetUser = commandType == CommandType.USER_CONTEXT ? resolvedEntities.users().get(targetId) : null;
        ResolvedMember targetMember = commandType == CommandType.USER_CONTEXT ? resolvedEntities.members().get(targetId) : null;
        ResolvedMessage targetMessage = commandType == CommandType.MESSAGE_CONTEXT ? resolvedEntities.messages().get(targetId) : null;

        return new CommandInteraction(
                data == null ? "" : stringOrEmpty(data.name()),
                commandType,
                parsed.group,
                parsed.subcommand,
                parsed.options,
                focusedOption(data == null ? null : data.options()),
                interactionPayload,
                context.guildId() == null,
                context.guildId(),
                context.userId(),
                Set.of(),
                Set.of(),
                targetUser,
                targetMember,
                null,
                null,
                null,
                targetMessage,
                resolvedEntities,
                parsed.optionUsers,
                parsed.optionMembers,
                parsed.optionChannels,
                parsed.optionRoles,
                parsed.optionAttachments
        );
    }

    private static Map<String, ResolvedMember> convertMembers(Map<String, ResolvedMember> members, Map<String, ResolvedUser> users) {
        if (members == null || members.isEmpty()) return Map.of();
        Map<String, ResolvedMember> converted = new HashMap<>();
        members.forEach((id, member) -> {
            if (id != null && member != null && users.containsKey(id)) {
                converted.put(id, new ResolvedMember(id, member.nickname()));
            }
        });
        return Map.copyOf(converted);
    }

    public InteractionExecution toComponentInteraction(DiscordInteractionPayload interaction, InteractionContext context, InteractionHandlerType type) {
        Objects.requireNonNull(type, "type");
        DiscordInteractionPayload payload = Objects.requireNonNull(interaction, "interaction");
        ResolvedEntities resolvedEntities = toResolvedEntities(payload.data() == null ? null : payload.data().resolved());
        return new InteractionExecution(
                type,
                textOrNull(payload.data() == null ? null : payload.data().customId()),
                Map.of(),
                interaction,
                resolvedEntities,
                context.guildId() == null,
                context.guildId(),
                context.userId(),
                Set.of(),
                Set.of(),
                extractStatePayload(textOrNull(payload.data() == null ? null : payload.data().customId()))
        );
    }

    public InteractionExecution toModalInteraction(DiscordInteractionPayload interaction, InteractionContext context) {
        Map<String, String> fields = new HashMap<>();
        DiscordInteractionPayload payload = Objects.requireNonNull(interaction, "interaction");
        var rows = payload.data() == null ? null : payload.data().components();
        if (rows != null) {
            for (DiscordInteractionPayload.ActionRow row : rows) {
                if (row.components() == null) continue;
                for (DiscordInteractionPayload.Component component : row.components()) {
                    String id = textOrNull(component.customId());
                    String value = textOrNull(component.value());
                    if (id != null && value != null) {
                        fields.put(id, value);
                    }
                }
            }
        }

        String customId = textOrNull(payload.data() == null ? null : payload.data().customId());
        ResolvedEntities resolvedEntities = toResolvedEntities(payload.data() == null ? null : payload.data().resolved());
        return new InteractionExecution(
                InteractionHandlerType.MODAL,
                customId,
                fields,
                interaction,
                resolvedEntities,
                context.guildId() == null,
                context.guildId(),
                context.userId(),
                Set.of(),
                Set.of(),
                extractStatePayload(customId)
        );
    }

    Integer componentType(DiscordInteractionPayload interaction) {
        Objects.requireNonNull(interaction, "interaction");
        return interaction.data() == null ? null : interaction.data().componentType();
    }

    private static final class ParsedData {
        private final Map<String, CommandOptionValue> options = new HashMap<>();
        private final Map<String, ResolvedUser> optionUsers = new HashMap<>();
        private final Map<String, ResolvedMember> optionMembers = new HashMap<>();
        private final Map<String, ResolvedChannel> optionChannels = new HashMap<>();
        private final Map<String, ResolvedRole> optionRoles = new HashMap<>();
        private final Map<String, ResolvedAttachment> optionAttachments = new HashMap<>();
        private String group;
        private String subcommand;

        private static <T> void putIfPresent(Map<String, T> map, String key, T value) {
            if (key != null && value != null) {
                map.put(key, value);
            }
        }

        private static String resolveEntityId(Object rawValue) {
            if (rawValue instanceof String value && !value.isBlank()) {
                return value;
            }
            if (rawValue instanceof Number number) {
                return Long.toString(number.longValue());
            }
            return null;
        }

        private void collectResolvedEntities(String optionName, DiscordOptionType optionType, Object value, ResolvedEntities resolved) {
            String entityId = resolveEntityId(value);
            if (entityId == null) {
                return;
            }
            switch (optionType) {
                case USER -> {
                    putIfPresent(optionUsers, optionName, resolved.users().get(entityId));
                    putIfPresent(optionMembers, optionName, resolved.members().get(entityId));
                }
                case CHANNEL -> putIfPresent(optionChannels, optionName, resolved.channels().get(entityId));
                case ROLE -> putIfPresent(optionRoles, optionName, resolved.roles().get(entityId));
                case ATTACHMENT -> putIfPresent(optionAttachments, optionName, resolved.attachments().get(entityId));
                case MENTIONABLE -> {
                    putIfPresent(optionUsers, optionName, resolved.users().get(entityId));
                    putIfPresent(optionMembers, optionName, resolved.members().get(entityId));
                    putIfPresent(optionRoles, optionName, resolved.roles().get(entityId));
                }
                case STRING, INTEGER, BOOLEAN, NUMBER, SUB_COMMAND, SUB_COMMAND_GROUP -> {
                    // no resolved entities for scalar options
                }
            }
        }
    }
}
