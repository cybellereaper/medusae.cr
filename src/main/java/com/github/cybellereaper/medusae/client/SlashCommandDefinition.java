package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SlashCommandDefinition(
        int type,
        String name,
        String description,
        List<SlashCommandOptionDefinition> options,
        String defaultMemberPermissions,
        Boolean dmPermission,
        Boolean nsfw
) {
    public static final int CHAT_INPUT = 1;
    public static final int USER = 2;
    public static final int MESSAGE = 3;

    public SlashCommandDefinition(String name, String description, List<SlashCommandOptionDefinition> options) {
        this(CHAT_INPUT, name, description, options, null, null, null);
    }

    public SlashCommandDefinition {
        Objects.requireNonNull(name, "name");
        options = options == null ? List.of() : List.copyOf(options);

        if (type < CHAT_INPUT || type > MESSAGE) {
            throw new IllegalArgumentException("Unsupported application command type: " + type);
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (type == CHAT_INPUT) {
            Objects.requireNonNull(description, "description");
            if (description.isBlank()) {
                throw new IllegalArgumentException("description must not be blank");
            }
        } else {
            if (!options.isEmpty()) {
                throw new IllegalArgumentException("Context menu commands do not support options");
            }
            if (description != null && !description.isBlank()) {
                throw new IllegalArgumentException("Context menu commands do not support description");
            }
            description = null;
        }

        if (defaultMemberPermissions != null && !defaultMemberPermissions.isBlank()
                && !defaultMemberPermissions.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("defaultMemberPermissions must be a numeric string");
        }
    }

    public static SlashCommandDefinition simple(String name, String description) {
        return new SlashCommandDefinition(name, description, List.of());
    }

    public static SlashCommandDefinition userContextMenu(String name) {
        return new SlashCommandDefinition(USER, name, null, List.of(), null, null, null);
    }

    public static SlashCommandDefinition messageContextMenu(String name) {
        return new SlashCommandDefinition(MESSAGE, name, null, List.of(), null, null, null);
    }

    public SlashCommandDefinition withDefaultMemberPermissions(long permissions) {
        return withDefaultMemberPermissions(DiscordPermissions.asString(permissions));
    }

    public SlashCommandDefinition withDefaultMemberPermissions(String permissionsBitset) {
        return new SlashCommandDefinition(type, name, description, options, permissionsBitset, dmPermission, nsfw);
    }

    public SlashCommandDefinition withDmPermission(boolean isAllowedInDm) {
        return new SlashCommandDefinition(type, name, description, options, defaultMemberPermissions, isAllowedInDm, nsfw);
    }

    public SlashCommandDefinition withNsfw(boolean isNsfw) {
        return new SlashCommandDefinition(type, name, description, options, defaultMemberPermissions, dmPermission, isNsfw);
    }

    public Map<String, Object> toRequestPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("name", name);

        if (type == CHAT_INPUT) {
            payload.put("description", description);
            if (!options.isEmpty()) {
                payload.put("options", options.stream().map(SlashCommandOptionDefinition::toRequestPayload).toList());
            }
        }

        if (defaultMemberPermissions != null && !defaultMemberPermissions.isBlank()) {
            payload.put("default_member_permissions", defaultMemberPermissions);
        }

        if (dmPermission != null) {
            payload.put("dm_permission", dmPermission);
        }

        if (nsfw != null) {
            payload.put("nsfw", nsfw);
        }

        return payload;
    }
}
