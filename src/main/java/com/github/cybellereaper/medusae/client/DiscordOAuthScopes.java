package com.github.cybellereaper.medusae.client;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * OAuth2 scopes commonly used for Discord app authorization.
 */
public final class DiscordOAuthScopes {
    public static final String BOT = "bot";
    public static final String APPLICATIONS_COMMANDS = "applications.commands";
    public static final String IDENTIFY = "identify";
    public static final String GUILDS = "guilds";
    public static final String EMAIL = "email";

    private DiscordOAuthScopes() {
    }

    public static String join(String... scopes) {
        Objects.requireNonNull(scopes, "scopes");

        Set<String> orderedScopes = new LinkedHashSet<>();
        for (String scope : scopes) {
            Objects.requireNonNull(scope, "scope");
            if (scope.isBlank()) {
                throw new IllegalArgumentException("scope must not be blank");
            }
            orderedScopes.add(scope);
        }

        if (orderedScopes.isEmpty()) {
            throw new IllegalArgumentException("at least one scope is required");
        }

        return String.join(" ", orderedScopes);
    }

    public static String[] defaultBotScopes() {
        return new String[]{BOT, APPLICATIONS_COMMANDS};
    }

    public static String normalize(String[] scopes) {
        Objects.requireNonNull(scopes, "scopes");
        return join(Arrays.copyOf(scopes, scopes.length));
    }
}
