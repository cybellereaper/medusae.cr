package com.github.cybellereaper.commands.discord;

import com.github.cybellereaper.commands.discord.adapter.DiscordFrameworkBinder;
import com.github.cybellereaper.examples.commands.ExampleCommandBootstrap;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DiscordFrameworkBinderTest {

    @Test
    void plansBindingsIncludingAutocompleteFlags() {
        var framework = ExampleCommandBootstrap.createFramework();
        Map<String, DiscordFrameworkBinder.CommandBinding> bindings = DiscordFrameworkBinder.planBindings(framework)
                .stream()
                .collect(Collectors.toMap(DiscordFrameworkBinder.CommandBinding::commandName, Function.identity()));

        assertEquals(3, bindings.size());
        assertTrue(bindings.get("user").hasAutocomplete());
        assertFalse(bindings.get("inspect user").hasAutocomplete());
        assertFalse(bindings.get("quote message").hasAutocomplete());
    }

    @Test
    void planBindingsRejectsNullFramework() {
        assertThrows(NullPointerException.class, () -> DiscordFrameworkBinder.planBindings(null));
    }
}
