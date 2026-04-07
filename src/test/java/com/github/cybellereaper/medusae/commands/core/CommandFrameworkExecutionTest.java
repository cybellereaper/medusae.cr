package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.client.ResolvedMember;
import com.github.cybellereaper.medusae.client.ResolvedUser;
import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import com.github.cybellereaper.medusae.commands.core.model.CommandInteraction;
import com.github.cybellereaper.medusae.commands.core.model.CommandOptionValue;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.model.ResolvedEntities;
import com.github.cybellereaper.medusae.commands.core.response.ImmediateResponse;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CommandFrameworkExecutionTest {

    @Test
    void executesSlashCommandWithTypedResolution() {
        CommandFramework framework = new CommandFramework();
        var handler = new GreetingCommand();
        framework.registerCommands(handler);

        AtomicReference<Object> capturedResponse = new AtomicReference<>();
        framework.execute(new CommandInteraction(
                "greet", CommandType.CHAT_INPUT, null, null,
                Map.of("name", new CommandOptionValue("Cybele", 3)),
                null, Map.of(), true, null, "u1", Set.of(), Set.of(),
                null, null, null, null, null, null
        ), response -> capturedResponse.set(response));

        assertEquals("Cybele", handler.lastName);
        assertInstanceOf(ImmediateResponse.class, capturedResponse.get());
    }

    @Test
    void failsFastWhenCheckIsMissing() {
        CommandFramework framework = new CommandFramework();
        assertThrows(RegistrationException.class, () -> framework.registerCommands(new CheckedCommand()));
    }

    @Test
    void executesCustomCheck() {
        CommandFramework framework = new CommandFramework();
        framework.registerCheck("allow", ctx -> true);
        framework.registerCommands(new CheckedCommand());

        assertDoesNotThrow(() -> framework.execute(new CommandInteraction(
                "checked", CommandType.CHAT_INPUT, null, null,
                Map.of(), null, Map.of(), true, null, "user", Set.of(), Set.of(),
                null, null, null, null, null, null
        ), response -> {}));
    }

    @Test
    void dispatchesAutocompleteProvider() {
        CommandFramework framework = new CommandFramework();
        framework.registerAutocomplete("reasons", (ctx, value) -> List.of(value + "1", value + "2"));
        framework.registerCommands(new AutoCommand());

        List<String> values = framework.executeAutocomplete(new CommandInteraction(
                "auto", CommandType.CHAT_INPUT, null, null,
                Map.of("reason", new CommandOptionValue("sp", 3)),
                "reason", Map.of(), true, null, "u", Set.of(), Set.of(),
                null, null, null, null, null, null
        ), response -> {});

        assertEquals(List.of("sp1", "sp2"), values);
    }


    @Test
    void resolvesSlashResolvedUserAndMemberOptions() {
        CommandFramework framework = new CommandFramework();
        var handler = new ResolvedEntityCommand();
        framework.registerCommands(handler);

        framework.execute(new CommandInteraction(
                "entities", CommandType.CHAT_INPUT, null, null,
                Map.of(), null, Map.of(), false, "g1", "u1", Set.of(), Set.of(),
                null, null, null, null, null, null,
                ResolvedEntities.empty(),
                Map.of("target", new ResolvedUser("42", "tester", null, false)),
                Map.of("target", new ResolvedMember("42", "TesterNick")),
                Map.of(), Map.of(), Map.of()
        ), response -> {});

        assertEquals("tester", handler.lastUser);
        assertEquals("42", handler.lastMemberId);
    }

    @Test
    void resolvesJavaOptionalOptionValues() {
        CommandFramework framework = new CommandFramework();
        var handler = new OptionalCommand();
        framework.registerCommands(handler);

        framework.execute(new CommandInteraction(
                "optional", CommandType.CHAT_INPUT, null, null,
                Map.of("reason", new CommandOptionValue("Spam", 3)),
                null, Map.of(), false, "g1", "u1", Set.of(), Set.of(),
                null, null, null, null, null, null
        ), response -> {});

        assertEquals(java.util.Optional.of("Spam"), handler.lastReason);
    }

    @Test
    void resolvesMissingJavaOptionalOptionAsEmpty() {
        CommandFramework framework = new CommandFramework();
        var handler = new OptionalCommand();
        framework.registerCommands(handler);

        framework.execute(new CommandInteraction(
                "optional", CommandType.CHAT_INPUT, null, null,
                Map.of(),
                null, Map.of(), false, "g1", "u1", Set.of(), Set.of(),
                null, null, null, null, null, null
        ), response -> {});

        assertEquals(java.util.Optional.empty(), handler.lastReason);
    }

    @Command("greet")
    static final class GreetingCommand {
        private String lastName;

        @Execute
        ImmediateResponse root(@Name("name") String name) {
            lastName = name;
            return ImmediateResponse.publicMessage("hello");
        }
    }

    @Command("checked")
    @Check("allow")
    static final class CheckedCommand {
        @Execute
        void root() {}
    }

    @Command("auto")
    static final class AutoCommand {
        @Execute
        void root(@Name("reason") @Autocomplete("reasons") String reason) {}
    }


    @Command("entities")
    static final class ResolvedEntityCommand {
        private String lastUser;
        private String lastMemberId;

        @Execute
        void root(@Name("target") ResolvedUser user,
                  @Name("target") ResolvedMember member) {
            lastUser = user == null ? null : user.username();
            lastMemberId = member == null ? null : member.userId();
        }
    }

    @Command("optional")
    static final class OptionalCommand {
        private java.util.Optional<String> lastReason = java.util.Optional.empty();

        @Execute
        void root(@Name("reason") java.util.Optional<String> reason) {
            lastReason = reason;
        }
    }

}
