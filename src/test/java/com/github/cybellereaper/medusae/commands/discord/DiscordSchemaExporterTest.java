package com.github.cybellereaper.medusae.commands.discord;

import com.github.cybellereaper.medusae.client.SlashCommandDefinition;
import com.github.cybellereaper.medusae.client.ResolvedChannel;
import com.github.cybellereaper.medusae.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.parser.CommandParser;
import com.github.cybellereaper.medusae.commands.discord.schema.DiscordCommandSchemaExporter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

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
    void exportsJavaOptionalAsNonRequiredOption() {
        CommandParser parser = new CommandParser();
        DiscordCommandSchemaExporter exporter = new DiscordCommandSchemaExporter();

        SlashCommandDefinition slash = exporter.exportDefinition(parser.parse(new OptionalCommand()));
        SlashCommandOptionDefinition option = slash.options().getFirst();
        assertEquals("reason", option.name());
        assertEquals(SlashCommandOptionDefinition.STRING, option.type());
        assertFalse(option.required());
    }

    @Test
    void exportsExtendedOptionMetadata() {
        CommandParser parser = new CommandParser();
        DiscordCommandSchemaExporter exporter = new DiscordCommandSchemaExporter();

        SlashCommandDefinition slash = exporter.exportDefinition(parser.parse(new AdvancedSchemaCommand()));
        SlashCommandOptionDefinition topic = slash.options().stream().filter(o -> o.name().equals("topic")).findFirst().orElseThrow();
        SlashCommandOptionDefinition count = slash.options().stream().filter(o -> o.name().equals("count")).findFirst().orElseThrow();
        SlashCommandOptionDefinition channel = slash.options().stream().filter(o -> o.name().equals("channel")).findFirst().orElseThrow();

        assertEquals(1, topic.choices().size());
        assertEquals(1, topic.minLength());
        assertEquals(16, topic.maxLength());
        assertEquals(Map.of("de", "thema"), topic.nameLocalizations());
        assertEquals(Map.of("de", "Beschreibung"), topic.descriptionLocalizations());

        assertEquals(5.0, count.minValue());
        assertEquals(10.0, count.maxValue());

        assertEquals(2, channel.channelTypes().size());

        Map<String, Object> payload = topic.toRequestPayload();
        assertTrue(payload.containsKey("choices"));
        assertTrue(channel.toRequestPayload().containsKey("channel_types"));
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

    @Command("schema")
    static final class AdvancedSchemaCommand {
        @Execute
        void root(
                @Name("topic")
                @OptionChoice(name = "General", value = "general")
                @MinLength(1)
                @MaxLength(16)
                @NameLocalization(locale = "de", value = "thema")
                @DescriptionLocalization(locale = "de", value = "Beschreibung")
                String topic,
                @Name("count")
                @OptionChoice(name = "Five", value = "5")
                @MinValue(5)
                @MaxValue(10)
                int count,
                @Name("channel")
                @ChannelTypes({0, 5})
                ResolvedChannel channel
        ) {
        }
    }
}
