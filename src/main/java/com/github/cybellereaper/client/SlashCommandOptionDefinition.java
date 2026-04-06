package com.github.cybellereaper.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SlashCommandOptionDefinition(
        int type,
        String name,
        String description,
        boolean required,
        boolean autocomplete,
        List<SlashCommandOptionDefinition> options
) {
    public static final int SUBCOMMAND = 1;
    public static final int SUBCOMMAND_GROUP = 2;
    public static final int STRING = 3;
    public static final int INTEGER = 4;
    public static final int BOOLEAN = 5;
    public static final int USER = 6;
    public static final int CHANNEL = 7;
    public static final int ROLE = 8;
    public static final int MENTIONABLE = 9;
    public static final int NUMBER = 10;
    public static final int ATTACHMENT = 11;

    public SlashCommandOptionDefinition(int type, String name, String description, boolean required, boolean autocomplete) {
        this(type, name, description, required, autocomplete, List.of());
    }

    public SlashCommandOptionDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        options = options == null ? List.of() : List.copyOf(options);

        if (type < SUBCOMMAND || type > ATTACHMENT) {
            throw new IllegalArgumentException("Unsupported slash command option type: " + type);
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
    }

    public static SlashCommandOptionDefinition string(String name, String description, boolean required) {
        return new SlashCommandOptionDefinition(STRING, name, description, required, false, List.of());
    }

    public static SlashCommandOptionDefinition autocompletedString(String name, String description, boolean required) {
        return new SlashCommandOptionDefinition(STRING, name, description, required, true, List.of());
    }

    public static SlashCommandOptionDefinition subcommand(String name, String description, List<SlashCommandOptionDefinition> options) {
        return new SlashCommandOptionDefinition(SUBCOMMAND, name, description, false, false, options);
    }

    public static SlashCommandOptionDefinition subcommandGroup(String name, String description, List<SlashCommandOptionDefinition> options) {
        return new SlashCommandOptionDefinition(SUBCOMMAND_GROUP, name, description, false, false, options);
    }

    public Map<String, Object> toRequestPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        payload.put("name", name);
        payload.put("description", description);

        if (type >= STRING) {
            payload.put("required", required);
            if (autocomplete) {
                payload.put("autocomplete", true);
            }
        }

        if (!options.isEmpty()) {
            payload.put("options", options.stream().map(SlashCommandOptionDefinition::toRequestPayload).toList());
        }
        return payload;
    }
}
