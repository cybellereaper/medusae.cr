package com.github.cybellereaper.examples.commands;

import com.github.cybellereaper.commands.core.execute.CommandFramework;

import java.util.List;

public final class ExampleCommandBootstrap {
    private ExampleCommandBootstrap() {
    }

    public static CommandFramework createFramework() {
        CommandFramework framework = new CommandFramework();

        framework.registerCheck("in-guild", ctx -> !ctx.interaction().dm());
        framework.registerAutocomplete("common-reasons", (ctx, input) -> {
            String prefix = input == null ? "" : input.toLowerCase();
            return List.of("Spam", "Harassment", "Raid", "Scam", "Phishing").stream()
                    .filter(reason -> reason.toLowerCase().startsWith(prefix))
                    .toList();
        });

        framework.registerCommands(
                new UserCommands(),
                new UserProfileContextCommands(),
                new MessageContextCommands()
        );

        return framework;
    }
}
