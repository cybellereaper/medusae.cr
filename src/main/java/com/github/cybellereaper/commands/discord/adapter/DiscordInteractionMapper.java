package com.github.cybellereaper.commands.discord.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.client.*;
import com.github.cybellereaper.commands.core.model.CommandInteraction;
import com.github.cybellereaper.commands.core.model.CommandOptionValue;
import com.github.cybellereaper.commands.core.model.CommandType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DiscordInteractionMapper {
    public CommandInteraction toCoreInteraction(JsonNode interaction, InteractionContext context) {
        Objects.requireNonNull(interaction, "interaction");
        Objects.requireNonNull(context, "context");

        JsonNode data = interaction.path("data");
        CommandType commandType = mapCommandType(data.path("type").asInt(1));

        ParsedData parsed = parseOptions(data.path("options"), data.path("resolved"), new ParsedData());

        String targetId = textOrNull(data.path("target_id"));
        ResolvedUser targetUser = commandType == CommandType.USER_CONTEXT && targetId != null ? context.resolvedUserValue(targetId) : null;
        ResolvedMember targetMember = commandType == CommandType.USER_CONTEXT && targetId != null ? context.resolvedMemberValue(targetId) : null;
        ResolvedMessage targetMessage = commandType == CommandType.MESSAGE_CONTEXT && targetId != null
                ? ResolvedMessage.from(data.path("resolved").path("messages").path(targetId))
                : null;

        return new CommandInteraction(
                data.path("name").asText(""),
                commandType,
                parsed.group,
                parsed.subcommand,
                parsed.options,
                focusedOption(data.path("options")),
                interaction,
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
                parsed.optionUsers,
                parsed.optionMembers,
                parsed.optionChannels,
                parsed.optionRoles,
                parsed.optionAttachments
        );
    }

    private static CommandType mapCommandType(int discordType) {
        return switch (discordType) {
            case 2 -> CommandType.USER_CONTEXT;
            case 3 -> CommandType.MESSAGE_CONTEXT;
            default -> CommandType.CHAT_INPUT;
        };
    }

    private static String focusedOption(JsonNode options) {
        if (!options.isArray()) {
            return null;
        }
        for (JsonNode option : options) {
            if (option.path("focused").asBoolean(false)) {
                return textOrNull(option.path("name"));
            }
            String nested = focusedOption(option.path("options"));
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static ParsedData parseOptions(JsonNode nodes, JsonNode globalResolved, ParsedData parsedData) {
        if (!nodes.isArray()) {
            return parsedData;
        }

        for (JsonNode option : nodes) {
            int type = option.path("type").asInt(0);
            String name = textOrNull(option.path("name"));
            if (name == null) {
                continue;
            }

            if (type == 1) {
                parsedData.subcommand = name;
                parseOptions(option.path("options"), globalResolved, parsedData);
                continue;
            }
            if (type == 2) {
                parsedData.group = name;
                JsonNode children = option.path("options");
                if (children.isArray() && !children.isEmpty()) {
                    JsonNode subcommandNode = children.get(0);
                    parsedData.subcommand = textOrNull(subcommandNode.path("name"));
                    parseOptions(subcommandNode.path("options"), globalResolved, parsedData);
                }
                continue;
            }

            Object rawValue = parseValue(option.path("value"));
            parsedData.options.put(name, new CommandOptionValue(rawValue, type));
            JsonNode optionResolved = option.path("resolved");
            JsonNode resolvedSource = optionResolved.isMissingNode() || optionResolved.isNull() ? globalResolved : optionResolved;
            parsedData.collectResolvedEntities(name, type, rawValue, resolvedSource, option.path("value"));
        }

        return parsedData;
    }

    private static Object parseValue(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isIntegralNumber()) {
            return value.asLong();
        }
        if (value.isFloatingPointNumber()) {
            return value.asDouble();
        }
        return textOrNull(value);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        String text = node.asText(null);
        return text == null || text.isBlank() ? null : text;
    }

    private static final class ParsedData {
        private String group;
        private String subcommand;
        private final Map<String, CommandOptionValue> options = new HashMap<>();
        private final Map<String, Object> optionUsers = new HashMap<>();
        private final Map<String, Object> optionMembers = new HashMap<>();
        private final Map<String, Object> optionChannels = new HashMap<>();
        private final Map<String, Object> optionRoles = new HashMap<>();
        private final Map<String, Object> optionAttachments = new HashMap<>();

        private void collectResolvedEntities(String optionName, int optionType, Object rawValue, JsonNode resolved, JsonNode rawOptionValue) {
            String entityId = resolveEntityId(rawValue, rawOptionValue);
            if (entityId == null) {
                return;
            }

            switch (optionType) {
                case 6 -> {
                    ResolvedUser user = ResolvedUser.from(resolved.path("users").path(entityId));
                    ResolvedMember member = ResolvedMember.from(entityId, resolved.path("members").path(entityId));
                    putIfPresent(optionUsers, optionName, user);
                    putIfPresent(optionMembers, optionName, member);
                }
                case 7 -> putIfPresent(optionChannels, optionName,
                        ResolvedChannel.from(resolved.path("channels").path(entityId)));
                case 8 -> putIfPresent(optionRoles, optionName,
                        ResolvedRole.from(resolved.path("roles").path(entityId)));
                case 11 -> putIfPresent(optionAttachments, optionName,
                        ResolvedAttachment.from(resolved.path("attachments").path(entityId)));
                default -> {
                    // not a resolved-entity option
                }
            }
        }


        private static <T> void putIfPresent(Map<String, Object> map, String key, T value) {
            if (key != null && value != null) {
                map.put(key, value);
            }
        }

        private static String resolveEntityId(Object rawValue, JsonNode rawOptionValue) {
            if (rawValue instanceof String value && !value.isBlank()) {
                return value;
            }
            if (rawValue instanceof Number number) {
                return Long.toString(number.longValue());
            }
            if (rawOptionValue != null && rawOptionValue.isIntegralNumber()) {
                return Long.toString(rawOptionValue.longValue());
            }
            return null;
        }
    }
}
