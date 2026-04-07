package com.github.cybellereaper.medusae.commands.core.model;

import com.github.cybellereaper.medusae.client.*;

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
        ResolvedUser targetUser,
        ResolvedMember targetMember,
        ResolvedChannel targetChannel,
        ResolvedRole targetRole,
        ResolvedAttachment targetAttachment,
        ResolvedMessage targetMessage,
        ResolvedEntities resolved,
        Map<String, ResolvedUser> optionUsers,
        Map<String, ResolvedMember> optionMembers,
        Map<String, ResolvedChannel> optionChannels,
        Map<String, ResolvedRole> optionRoles,
        Map<String, ResolvedAttachment> optionAttachments
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
        resolved = resolved == null ? ResolvedEntities.empty() : resolved;
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
            ResolvedUser targetUser,
            ResolvedMember targetMember,
            ResolvedChannel targetChannel,
            ResolvedRole targetRole,
            ResolvedAttachment targetAttachment,
            ResolvedMessage targetMessage
    ) {
        this(commandName, commandType, subcommandGroup, subcommand, options, focusedOption, rawInteraction, dm, guildId, userId,
                userPermissions, botPermissions, targetUser, targetMember, targetChannel, targetRole, targetAttachment,
                targetMessage, ResolvedEntities.empty(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
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

    public String routeKey() {
        if (subcommandGroup != null && subcommand != null) {
            return subcommandGroup + "/" + subcommand;
        }
        return subcommand;
    }
}
