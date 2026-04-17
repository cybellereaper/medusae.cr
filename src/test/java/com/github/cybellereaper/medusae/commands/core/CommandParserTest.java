package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.client.ResolvedChannel;
import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.model.CommandDefinition;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.parser.CommandParser;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {
    private final CommandParser parser = new CommandParser();

    @Test
    void parsesSlashTree() {
        CommandDefinition definition = parser.parse(new AdminCommands());

        assertEquals("admin", definition.name());
        assertEquals(CommandType.CHAT_INPUT, definition.type());
        assertEquals(2, definition.handlers().size());
        assertEquals("mod/ban", definition.handlers().getFirst().routeKey());
    }

    @Test
    void rejectsDuplicateSubcommandRoutes() {
        assertThrows(RegistrationException.class, () -> parser.parse(new DuplicateRoutes()));
    }

    @Test
    void treatsJavaOptionalAsOptionalOption() {
        CommandDefinition definition = parser.parse(new OptionalCommand());
        var parameter = definition.handlers().getFirst().parameters().getFirst();

        assertFalse(parameter.required());
        assertEquals("reason", parameter.optionName());
        assertEquals(String.class, parameter.optionType());
    }

    @Test
    void parsesExtendedOptionMetadata() {
        CommandDefinition definition = parser.parse(new SchemaCommand());
        var parameters = definition.handlers().getFirst().parameters();
        var topic = parameters.stream().filter(p -> p.optionName().equals("topic")).findFirst().orElseThrow();
        var channel = parameters.stream().filter(p -> p.optionName().equals("channel")).findFirst().orElseThrow();

        assertEquals(1, topic.choices().size());
        assertEquals(2, topic.minLength());
        assertEquals(12, topic.maxLength());
        assertEquals(Map.of("de", "thema"), topic.nameLocalizations());
        assertEquals(Map.of("de", "Beschreibung"), topic.descriptionLocalizations());
        assertEquals(2, channel.channelTypes().size());
    }

    @Test
    void rejectsChoicesOnAutocompleteOptions() {
        assertThrows(RegistrationException.class, () -> parser.parse(new InvalidChoiceAutocompleteCommand()));
    }

    @Command("admin")
    @Description("admin commands")
    static final class AdminCommands {
        @SubcommandGroup("mod")
        @Subcommand("ban")
        void ban(String user) {
        }

        @Subcommand("kick")
        void kick(String user) {
        }
    }

    @Command("dup")
    static final class DuplicateRoutes {
        @Subcommand("ping")
        void a() {}

        @Subcommand("ping")
        void b() {}
    }

    @Command("optional")
    static final class OptionalCommand {
        @Execute
        void root(@Name("reason") Optional<String> reason) {}
    }

    @Command("schema")
    static final class SchemaCommand {
        @Execute
        void root(
                @Name("topic")
                @OptionChoice(name = "General", value = "general")
                @MinLength(2)
                @MaxLength(12)
                @NameLocalization(locale = "de", value = "thema")
                @DescriptionLocalization(locale = "de", value = "Beschreibung")
                String topic,
                @Name("channel")
                @ChannelTypes({0, 5})
                ResolvedChannel channel
        ) {}
    }

    @Command("invalid")
    static final class InvalidChoiceAutocompleteCommand {
        @Execute
        void root(@OptionChoice(name = "A", value = "a") @Autocomplete("test") String value) {}
    }
}
