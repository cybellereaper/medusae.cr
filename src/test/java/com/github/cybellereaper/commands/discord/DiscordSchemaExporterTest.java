package com.github.cybellereaper.commands.discord;

import com.github.cybellereaper.client.SlashCommandDefinition;
import com.github.cybellereaper.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.commands.core.annotation.Command;
import com.github.cybellereaper.commands.core.annotation.Description;
import com.github.cybellereaper.commands.core.annotation.Execute;
import com.github.cybellereaper.commands.core.annotation.Autocomplete;
import com.github.cybellereaper.commands.core.annotation.Name;
import com.github.cybellereaper.commands.core.annotation.Optional;
import com.github.cybellereaper.commands.core.annotation.Subcommand;
import com.github.cybellereaper.commands.core.model.CommandType;
import com.github.cybellereaper.commands.core.parser.CommandParser;
import com.github.cybellereaper.commands.discord.schema.DiscordCommandSchemaExporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void exportsRequiredOptionsBeforeOptionalOptions() {
        CommandParser parser = new CommandParser();
        DiscordCommandSchemaExporter exporter = new DiscordCommandSchemaExporter();

        SlashCommandDefinition slash = exporter.exportDefinition(parser.parse(new MixedRequiredCommand()));

        assertEquals("target", slash.options().get(0).name());
        assertTrue(slash.options().get(0).required());
        assertEquals("reason", slash.options().get(1).name());
        assertFalse(slash.options().get(1).required());
        assertTrue(slash.options().get(1).autocomplete());
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

    @Command("ban")
    static final class MixedRequiredCommand {
        @Execute
        void root(@Optional @Autocomplete("reasons") String reason,
                  String target) {
        }
    }
}
