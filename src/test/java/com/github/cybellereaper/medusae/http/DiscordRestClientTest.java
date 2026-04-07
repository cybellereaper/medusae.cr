package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.DiscordClientConfig;
import com.github.cybellereaper.medusae.client.SlashCommandDefinition;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscordRestClientTest {
    private final DiscordRestClient restClient = new DiscordRestClient(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            DiscordClientConfig.builder("token").build()
    );

    @Test
    void rejectsBlankGuildIdForGuildCommandRegistration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> restClient.createGuildApplicationCommand("app-id", " ", SlashCommandDefinition.simple("ping", "Pong"))
        );
    }

    @Test
    void rejectsBlankApplicationIdForGlobalCommandRegistration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> restClient.createGlobalApplicationCommand("", SlashCommandDefinition.simple("ping", "Pong"))
        );
    }
}
