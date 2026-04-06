package com.github.cybellereaper.commands.core.check;

import com.github.cybellereaper.commands.core.execute.CommandContext;

@FunctionalInterface
public interface CommandCheck {
    boolean test(CommandContext context);
}
