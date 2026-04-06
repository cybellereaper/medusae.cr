package com.github.cybellereaper.commands.core.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record CommandInteraction(
        String commandName,
        CommandType commandType,
        String subcommandGroup,
        String subcommand,
        Map<String, CommandOptionValue> options,
        String focusedOption,
        Object rawInteraction,
        boolean dm,
        String guildId,
        String userId,
        Set<String> userPermissions,
        Set<String> botPermissions,
        Object targetUser,
        Object targetMember,
        Object targetChannel,
        Object targetRole,
        Object targetAttachment,
        Object targetMessage,
        Map<String, Object> optionUsers,
        Map<String, Object> optionMembers,
        Map<String, Object> optionChannels,
        Map<String, Object> optionRoles,
        Map<String, Object> optionAttachments
) {
    public CommandInteraction {
        options = immutableWithoutNulls(options);
        userPermissions = userPermissions == null ? Set.of() : Set.copyOf(userPermissions);
        botPermissions = botPermissions == null ? Set.of() : Set.copyOf(botPermissions);
        optionUsers = immutableWithoutNulls(optionUsers);
        optionMembers = immutableWithoutNulls(optionMembers);
        optionChannels = immutableWithoutNulls(optionChannels);
        optionRoles = immutableWithoutNulls(optionRoles);
        optionAttachments = immutableWithoutNulls(optionAttachments);
    }

    public CommandInteraction(
            String commandName,
            CommandType commandType,
            String subcommandGroup,
            String subcommand,
            Map<String, CommandOptionValue> options,
            String focusedOption,
            Object rawInteraction,
            boolean dm,
            String guildId,
            String userId,
            Set<String> userPermissions,
            Set<String> botPermissions,
            Object targetUser,
            Object targetMember,
            Object targetChannel,
            Object targetRole,
            Object targetAttachment,
            Object targetMessage
    ) {
        this(commandName, commandType, subcommandGroup, subcommand, options, focusedOption, rawInteraction, dm, guildId, userId,
                userPermissions, botPermissions, targetUser, targetMember, targetChannel, targetRole, targetAttachment,
                targetMessage, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public String routeKey() {
        if (subcommandGroup != null && subcommand != null) {
            return subcommandGroup + "/" + subcommand;
        }
        return subcommand;
    }

    private static <K, V> Map<K, V> immutableWithoutNulls(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<K, V> cleaned = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                cleaned.put(entry.getKey(), entry.getValue());
            }
        }

        if (cleaned.isEmpty()) {
            return Map.of();
        }

        return Map.copyOf(cleaned);
    }
}
