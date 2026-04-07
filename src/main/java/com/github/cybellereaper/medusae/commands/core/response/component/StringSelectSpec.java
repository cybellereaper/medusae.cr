package com.github.cybellereaper.medusae.commands.core.response.component;

import java.util.List;

public record StringSelectSpec(String customId, List<Option> options, String placeholder, Integer minValues,
                               Integer maxValues, boolean disabled) implements ComponentSpec {
    public static StringSelectSpec of(String customId, List<Option> options) {
        return new StringSelectSpec(customId, options, null, null, null, false);
    }

    public StringSelectSpec withPlaceholder(String value) {
        return new StringSelectSpec(customId, options, value, minValues, maxValues, disabled);
    }

    public StringSelectSpec withRange(Integer min, Integer max) {
        return new StringSelectSpec(customId, options, placeholder, min, max, disabled);
    }

    public StringSelectSpec disable() {
        return disabled ? this : new StringSelectSpec(customId, options, placeholder, minValues, maxValues, true);
    }

    public record Option(String label, String value, String description, boolean defaultSelected) {
        public static Option of(String label, String value) {
            return new Option(label, value, null, false);
        }
    }
}
