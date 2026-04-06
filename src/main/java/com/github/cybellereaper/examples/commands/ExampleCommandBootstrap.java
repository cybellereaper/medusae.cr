package com.github.cybellereaper.examples.commands;

import com.github.cybellereaper.commands.core.execute.CommandFramework;

import java.util.List;

public final class ExampleCommandBootstrap {
    private static final List<String> COMMON_REASONS = List.of("Spam", "Harassment", "Raid", "Scam", "Phishing");

    private ExampleCommandBootstrap() {
    }

    public static CommandFramework createFramework() {
        CommandFramework framework = new CommandFramework();

        registerCoreCapabilities(framework);
        registerShowcaseCommands(framework);
        registerInteractionModules(framework);

        return framework;
    }

    private static void registerCoreCapabilities(CommandFramework framework) {
        framework.registerCheck("in-guild", ctx -> !ctx.interaction().dm());
        framework.registerAutocomplete("common-reasons", (ctx, input) -> {
            String prefix = input == null ? "" : input.toLowerCase();
            return COMMON_REASONS.stream()
                    .filter(reason -> reason.toLowerCase().startsWith(prefix))
                    .toList();
        });
    }

    private static void registerShowcaseCommands(CommandFramework framework) {
        framework.registerCommands(
                new UserCommands(),
                new UserProfileContextCommands(),
                new MessageContextCommands()
        );
    }

    private static void registerInteractionModules(CommandFramework framework) {
        framework.registerModules(new TicketInteractionCommands());
    }
}
