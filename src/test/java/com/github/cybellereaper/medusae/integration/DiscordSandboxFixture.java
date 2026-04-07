package com.github.cybellereaper.medusae.integration;

import com.github.cybellereaper.medusae.client.DiscordClientConfig;

import java.util.Optional;

public final class DiscordSandboxFixture {
    private DiscordSandboxFixture() {
    }

    public static Optional<DiscordClientConfig> fromEnvironment() {
        String token = System.getenv("DISCORD_SANDBOX_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        String baseUrl = System.getenv().getOrDefault("DISCORD_SANDBOX_API_BASE_URL", "https://discord.com/api");
        return Optional.of(
                DiscordClientConfig.builder(token)
                        .apiBaseUrl(baseUrl)
                        .build()
        );
    }
}
