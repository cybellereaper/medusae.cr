package com.github.cybellereaper.medusae.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscordOAuthScopesTest {

    @Test
    void joinDeduplicatesAndPreservesOrder() {
        String scopes = DiscordOAuthScopes.join(
                DiscordOAuthScopes.BOT,
                DiscordOAuthScopes.APPLICATIONS_COMMANDS,
                DiscordOAuthScopes.BOT
        );

        assertEquals("bot applications.commands", scopes);
    }

    @Test
    void defaultBotScopesContainsExpectedValues() {
        assertArrayEquals(
                new String[]{"bot", "applications.commands"},
                DiscordOAuthScopes.defaultBotScopes()
        );
    }

    @Test
    void joinRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> DiscordOAuthScopes.join());
        assertThrows(IllegalArgumentException.class, () -> DiscordOAuthScopes.join(" "));
    }
}
