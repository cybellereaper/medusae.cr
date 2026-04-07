package com.github.cybellereaper.medusae.commands.discord.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.*;
import com.github.cybellereaper.medusae.commands.core.model.*;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayloadReader;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordOptionType;

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

    private static Object parseOptionValue(DiscordOptionType type, JsonNode rawValue) {
        if (rawValue == null || rawValue.isNull() || rawValue.isMissingNode()) {
            return null;
        }
        return switch (type) {
            case STRING -> rawValue.asText();
            case INTEGER -> parseLong(rawValue);
            case BOOLEAN -> parseBoolean(rawValue);
            case USER, CHANNEL, ROLE, MENTIONABLE, ATTACHMENT -> parseEntityId(rawValue);
            case NUMBER -> parseDouble(rawValue);
            case SUB_COMMAND, SUB_COMMAND_GROUP ->
                    throw new IllegalStateException("Subcommand options do not expose scalar values");
        };
    }

    private static String parseEntityId(JsonNode rawValue) {
        if (rawValue.isTextual()) {
            String value = rawValue.asText().trim();
            return value.isEmpty() ? null : value;
        }
        if (rawValue.isIntegralNumber()) {
            return Long.toString(rawValue.longValue());
        }
        return null;
    }

    private static Long parseLong(JsonNode rawValue) {
        if (rawValue.isIntegralNumber()) {
            return rawValue.longValue();
        }
        if (rawValue.isTextual()) {
            try {
                return Long.parseLong(rawValue.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Double parseDouble(JsonNode rawValue) {
        if (rawValue.isNumber()) {
            return rawValue.doubleValue();
        }
        if (rawValue.isTextual()) {
            try {
                return Double.parseDouble(rawValue.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Boolean parseBoolean(JsonNode rawValue) {
        if (rawValue.isBoolean()) {
            return rawValue.booleanValue();
        }
        if (rawValue.isTextual()) {
            String text = rawValue.asText().trim();
            if ("true".equalsIgnoreCase(text)) return true;
            if ("false".equalsIgnoreCase(text)) return false;
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
        Map<String, ResolvedUser> users = convertUsers(resolved.users());
        return new ResolvedEntities(
                users,
                convertMembers(resolved.members(), users),
                convertRoles(resolved.roles()),
                convertChannels(resolved.channels()),
                convertMessages(resolved.messages()),
                convertAttachments(resolved.attachments())
        );
    }

    private static Map<String, ResolvedUser> convertUsers(Map<String, JsonNode> users) {
        if (users == null || users.isEmpty()) return Map.of();
        Map<String, ResolvedUser> converted = new HashMap<>();
        users.forEach((id, node) -> putIfPresent(converted, id, ResolvedUser.from(node)));
        return Map.copyOf(converted);
    }

    private static Map<String, ResolvedMember> convertMembers(Map<String, JsonNode> members, Map<String, ResolvedUser> users) {
        if (members == null || members.isEmpty()) return Map.of();
        Map<String, ResolvedMember> converted = new HashMap<>();
        members.forEach((id, node) -> {
            if (users.containsKey(id)) {
                putIfPresent(converted, id, ResolvedMember.from(id, node));
            }
        });
        return Map.copyOf(converted);
    }

    private static Map<String, ResolvedRole> convertRoles(Map<String, JsonNode> roles) {
        if (roles == null || roles.isEmpty()) return Map.of();
        Map<String, ResolvedRole> converted = new HashMap<>();
        roles.forEach((id, node) -> putIfPresent(converted, id, ResolvedRole.from(node)));
        return Map.copyOf(converted);
    }

    private static Map<String, ResolvedChannel> convertChannels(Map<String, JsonNode> channels) {
        if (channels == null || channels.isEmpty()) return Map.of();
        Map<String, ResolvedChannel> converted = new HashMap<>();
        channels.forEach((id, node) -> putIfPresent(converted, id, ResolvedChannel.from(node)));
        return Map.copyOf(converted);
    }

    private static Map<String, ResolvedMessage> convertMessages(Map<String, JsonNode> messages) {
        if (messages == null || messages.isEmpty()) return Map.of();
        Map<String, ResolvedMessage> converted = new HashMap<>();
        messages.forEach((id, node) -> putIfPresent(converted, id, ResolvedMessage.from(node)));
        return Map.copyOf(converted);
    }

    private static Map<String, ResolvedAttachment> convertAttachments(Map<String, JsonNode> attachments) {
        if (attachments == null || attachments.isEmpty()) return Map.of();
        Map<String, ResolvedAttachment> converted = new HashMap<>();
        attachments.forEach((id, node) -> putIfPresent(converted, id, ResolvedAttachment.from(node)));
        return Map.copyOf(converted);
    }

    private static <T> void putIfPresent(Map<String, T> target, String id, T value) {
        if (id != null && value != null) {
            target.put(id, value);
        }
    }

    public CommandInteraction toCoreInteraction(JsonNode interaction, InteractionContext context) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(context, "context");
        return toCoreInteraction(payloadReader.read(interaction), interaction, context);
    }

    CommandInteraction toCoreInteraction(DiscordInteractionPayload interactionPayload, JsonNode rawInteraction, InteractionContext context) {
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
                rawInteraction,
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

    public InteractionExecution toComponentInteraction(JsonNode interaction, InteractionContext context, InteractionHandlerType type) {
        Objects.requireNonNull(type, "type");
        DiscordInteractionPayload payload = payloadReader.read(interaction);
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

    public InteractionExecution toModalInteraction(JsonNode interaction, InteractionContext context) {
        Map<String, String> fields = new HashMap<>();
        DiscordInteractionPayload payload = payloadReader.read(interaction);
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

    Integer componentType(JsonNode interaction) {
        DiscordInteractionPayload payload = payloadReader.read(interaction);
        return payload.data() == null ? null : payload.data().componentType();
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
