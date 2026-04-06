package com.github.cybellereaper.commands.discord;

import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.discord.adapter.DiscordCommandBinder;
import com.github.cybellereaper.examples.commands.ExampleCommandBootstrap;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DiscordCommandBinderTest {

    @Test
    void plansBindingsIncludingAutocompleteFlag() {
        CommandFramework framework = ExampleCommandBootstrap.createFramework();
        DiscordCommandBinder binder = new DiscordCommandBinder();

        Map<String, DiscordCommandBinder.Binding> planned = binder.planBindings(framework).stream()
                .collect(Collectors.toMap(DiscordCommandBinder.Binding::commandName, Function.identity()));

        assertEquals(3, planned.size());
        assertTrue(planned.get("user").enableAutocomplete());
        assertFalse(planned.get("inspect user").enableAutocomplete());
        assertFalse(planned.get("quote message").enableAutocomplete());
    }
}
