package com.github.cybellereaper.commands.core;

import com.github.cybellereaper.commands.core.annotation.*;
import com.github.cybellereaper.commands.core.exception.RegistrationException;
import com.github.cybellereaper.commands.core.model.CommandDefinition;
import com.github.cybellereaper.commands.core.model.CommandType;
import com.github.cybellereaper.commands.core.model.CommandParameter;
import com.github.cybellereaper.commands.core.parser.CommandParser;
import org.junit.jupiter.api.Test;

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
    void preservesDeclaredParameterNamesWithoutNameAnnotation() {
        CommandDefinition definition = parser.parse(new NamedByCompilerCommand());

        CommandParameter reason = definition.handlers().getFirst().parameters().get(1);
        assertEquals("reason", reason.optionName());
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

    @Command("named")
    static final class NamedByCompilerCommand {
        @Execute
        void root(String target, @Optional String reason) {
        }
    }
}
