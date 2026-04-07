package com.github.cybellereaper.medusae.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record DiscordStringSelectMenu(
        String customId,
        List<DiscordSelectOption> options,
        String placeholder,
        Integer minValues,
        Integer maxValues,
        boolean disabled
) implements DiscordComponent {
    public DiscordStringSelectMenu {
        Objects.requireNonNull(customId, "customId");
        if (customId.isBlank()) {
            throw new IllegalArgumentException("customId must not be blank");
        }

        Objects.requireNonNull(options, "options");
        options = options.stream().filter(Objects::nonNull).toList();
        if (options.isEmpty()) {
            throw new IllegalArgumentException("options must not be empty");
        }

        if (minValues != null && minValues < 0) {
            throw new IllegalArgumentException("minValues must be >= 0");
        }
        if (maxValues != null && maxValues < 1) {
            throw new IllegalArgumentException("maxValues must be >= 1");
        }
        if (minValues != null && maxValues != null && minValues > maxValues) {
            throw new IllegalArgumentException("minValues cannot be greater than maxValues");
        }
    }

    public static DiscordStringSelectMenu of(String customId, List<DiscordSelectOption> options) {
        return new DiscordStringSelectMenu(customId, options, null, null, null, false);
    }

    public DiscordStringSelectMenu withPlaceholder(String placeholder) {
        return new DiscordStringSelectMenu(customId, options, placeholder, minValues, maxValues, disabled);
    }

    public DiscordStringSelectMenu withSelectionRange(Integer minValues, Integer maxValues) {
        return new DiscordStringSelectMenu(customId, options, placeholder, minValues, maxValues, disabled);
    }

    public DiscordStringSelectMenu disable() {
        return disabled ? this : new DiscordStringSelectMenu(customId, options, placeholder, minValues, maxValues, true);
    }

    @Override
    public Map<String, Object> toPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", 3);
        payload.put("custom_id", customId);
        payload.put("options", options.stream().map(DiscordSelectOption::toPayload).toList());

        if (placeholder != null && !placeholder.isBlank()) {
            payload.put("placeholder", placeholder);
        }
        if (minValues != null) {
            payload.put("min_values", minValues);
        }
        if (maxValues != null) {
            payload.put("max_values", maxValues);
        }
        if (disabled) {
            payload.put("disabled", true);
        }

        return payload;
    }
}
