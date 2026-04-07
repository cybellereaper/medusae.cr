package com.github.cybellereaper.medusae.commands.core.check;

import com.github.cybellereaper.medusae.commands.core.execute.CommandContext;

@FunctionalInterface
public interface CommandCheck {
    boolean test(CommandContext context);
}
