package com.github.cybellereaper.medusae.client;

import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;
import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordOptionValue;

import java.util.List;
import java.util.Objects;

final class InteractionOptionReader {
    static final String OPTIONS_FIELD = "options";

    private static final String NAME_FIELD = "name";
    private static final String VALUE_FIELD = "value";

    private final List<DiscordInteractionPayload.Option> options;

    InteractionOptionReader(List<DiscordInteractionPayload.Option> options) {
        this.options = options;
    }

    String optionString(String optionName) {
        DiscordInteractionPayload.Option option = findOptionNode(optionName);
        return option == null ? null : textOrNull(option.value());
    }

    Long optionLong(String optionName) {
        Object value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    Integer optionInt(String optionName) {
        Long value = optionLong(optionName);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    Boolean optionBoolean(String optionName) {
        Object value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String raw) {
            String text = raw.trim();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return null;
    }

    Double optionDouble(String optionName) {
        Object value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    String optionResolvedId(String optionName) {
        Object value = optionValue(optionName);
        if (value == null) {
            return null;
        }
        if (value instanceof String raw) {
            String text = raw.trim();
            return text.isEmpty() ? null : text;
        }
        if (value instanceof Number number) {
            return Long.toString(number.longValue());
        }
        return null;
    }

    private Object optionValue(String optionName) {
        DiscordInteractionPayload.Option option = findOptionNode(optionName);
        return option == null ? null : unwrap(option.value());
    }

    private DiscordInteractionPayload.Option findOptionNode(String optionName) {
        Objects.requireNonNull(optionName, "optionName");
        return findOptionNode(optionName, options);
    }

    private static DiscordInteractionPayload.Option findOptionNode(String optionName, List<DiscordInteractionPayload.Option> candidateOptions) {
        if (candidateOptions == null || candidateOptions.isEmpty()) {
            return null;
        }
        for (DiscordInteractionPayload.Option option : candidateOptions) {
            if (optionName.equals(option.name())) {
                return option;
            }
            DiscordInteractionPayload.Option nested = findOptionNode(optionName, option.options());
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    static String textOrNull(Object value) {
        value = unwrap(value);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private static Object unwrap(Object value) {
        return value instanceof DiscordOptionValue optionValue ? optionValue.value() : value;
    }
}
