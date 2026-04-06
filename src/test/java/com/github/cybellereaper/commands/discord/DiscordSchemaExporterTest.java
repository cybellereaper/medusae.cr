package com.github.cybellereaper.commands.discord;

import com.github.cybellereaper.client.SlashCommandDefinition;
import com.github.cybellereaper.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.commands.core.annotation.Command;
import com.github.cybellereaper.commands.core.annotation.Description;
import com.github.cybellereaper.commands.core.annotation.Execute;
import com.github.cybellereaper.commands.core.annotation.Name;
import com.github.cybellereaper.commands.core.annotation.Subcommand;
import com.github.cybellereaper.commands.core.model.CommandType;
import com.github.cybellereaper.commands.core.parser.CommandParser;
import com.github.cybellereaper.commands.discord.schema.DiscordCommandSchemaExporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
