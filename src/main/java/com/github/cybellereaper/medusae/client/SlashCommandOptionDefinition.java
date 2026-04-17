package com.github.cybellereaper.medusae.client;

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
        List<SlashCommandOptionDefinition> options,
        List<SlashCommandOptionChoice> choices,
        Double minValue,
        Double maxValue,
        Integer minLength,
        Integer maxLength,
        List<Integer> channelTypes,
        Map<String, String> nameLocalizations,
        Map<String, String> descriptionLocalizations
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

    public SlashCommandOptionDefinition(int type, String name, String description, boolean required, boolean autocomplete, List<SlashCommandOptionDefinition> options) {
        this(type, name, description, required, autocomplete, options, List.of(), null, null, null, null, List.of(), Map.of(), Map.of());
    }

    public SlashCommandOptionDefinition {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        options = options == null ? List.of() : List.copyOf(options);
        choices = choices == null ? List.of() : List.copyOf(choices);
        channelTypes = channelTypes == null ? List.of() : List.copyOf(channelTypes);
        nameLocalizations = nameLocalizations == null ? Map.of() : Map.copyOf(nameLocalizations);
        descriptionLocalizations = descriptionLocalizations == null ? Map.of() : Map.copyOf(descriptionLocalizations);

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

        if (!nameLocalizations.isEmpty()) {
            payload.put("name_localizations", nameLocalizations);
        }
        if (!descriptionLocalizations.isEmpty()) {
            payload.put("description_localizations", descriptionLocalizations);
        }

        if (type >= STRING) {
            payload.put("required", required);
            if (autocomplete) {
                payload.put("autocomplete", true);
            }
        }

        if (!choices.isEmpty()) {
            payload.put("choices", choices.stream().map(SlashCommandOptionChoice::toRequestPayload).toList());
        }
        if (minValue != null) {
            payload.put("min_value", minValue);
        }
        if (maxValue != null) {
            payload.put("max_value", maxValue);
        }
        if (minLength != null) {
            payload.put("min_length", minLength);
        }
        if (maxLength != null) {
            payload.put("max_length", maxLength);
        }
        if (!channelTypes.isEmpty()) {
            payload.put("channel_types", channelTypes);
        }

        if (!options.isEmpty()) {
            payload.put("options", options.stream().map(SlashCommandOptionDefinition::toRequestPayload).toList());
        }
        return payload;
    }
}
