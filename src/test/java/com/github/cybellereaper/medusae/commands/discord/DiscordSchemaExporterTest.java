package com.github.cybellereaper.medusae.commands.discord;

import com.github.cybellereaper.medusae.client.SlashCommandDefinition;
import com.github.cybellereaper.medusae.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.medusae.commands.core.annotation.Command;
import com.github.cybellereaper.medusae.commands.core.annotation.Description;
import com.github.cybellereaper.medusae.commands.core.annotation.Execute;
import com.github.cybellereaper.medusae.commands.core.annotation.Name;
import com.github.cybellereaper.medusae.commands.core.annotation.Subcommand;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.parser.CommandParser;
import com.github.cybellereaper.medusae.commands.discord.schema.DiscordCommandSchemaExporter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DiscordSchemaExporterTest {
    @Test
    void exportsContextMenusAndSubcommands() {
        CommandParser parser = new CommandParser();
        DiscordCommandSchemaExporter exporter = new DiscordCommandSchemaExporter();

        SlashCommandDefinition slash = exporter.exportDefinition(parser.parse(new SubcommandCommand()));
        assertEquals(SlashCommandDefinition.CHAT_INPUT, slash.type());
        assertEquals(SlashCommandOptionDefinition.SUBCOMMAND, slash.options().getFirst().type());

        SlashCommandDefinition user = exporter.exportDefinition(parser.parse(new UserMenuCommand()));
        assertEquals(SlashCommandDefinition.USER, user.type());
    }

    @Test
    void exportsJavaOptionalAsNonRequiredOption() {
        CommandParser parser = new CommandParser();
        DiscordCommandSchemaExporter exporter = new DiscordCommandSchemaExporter();

        SlashCommandDefinition slash = exporter.exportDefinition(parser.parse(new OptionalCommand()));
        SlashCommandOptionDefinition option = slash.options().getFirst();
        assertEquals("reason", option.name());
        assertEquals(SlashCommandOptionDefinition.STRING, option.type());
        assertFalse(option.required());
    }

    @Command("mod")
    @Description("moderation")
    static final class SubcommandCommand {
        @Subcommand("ban")
        void ban(@Name("target") String target) {
        }
    }

    @Command(value = "userinfo", type = CommandType.USER_CONTEXT)
    static final class UserMenuCommand {
        @Execute
        void root() {
        }
    }

    @Command("optional")
    static final class OptionalCommand {
        @Execute
        void root(@Name("reason") Optional<String> reason) {}
    }
}
