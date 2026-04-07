package com.github.cybellereaper.medusae.commands.core.response.component;

import java.util.Arrays;
import java.util.List;

public record ActionRowSpec(List<ComponentSpec> components) {
    public static ActionRowSpec of(ComponentSpec... components) {
        return new ActionRowSpec(Arrays.stream(components).toList());
    }
}
