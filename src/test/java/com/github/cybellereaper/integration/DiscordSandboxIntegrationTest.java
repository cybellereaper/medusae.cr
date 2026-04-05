package com.github.cybellereaper.integration;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DiscordSandboxIntegrationTest {

    @Test
    void canFetchCurrentApplicationAgainstSandbox() {
        Optional<DiscordClientConfig> maybeConfig = DiscordSandboxFixture.fromEnvironment();
        assumeTrue(maybeConfig.isPresent(), "set DISCORD_SANDBOX_BOT_TOKEN to enable integration tests");

        try (DiscordClient client = DiscordClient.create(maybeConfig.get())) {
            assertDoesNotThrow(() -> client.api().getCurrentApplication());
        }
    }
}
