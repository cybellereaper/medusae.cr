package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.http.DiscordRestClient;
import com.github.cybellereaper.medusae.http.RateLimitObserver;
import com.github.cybellereaper.medusae.http.RetryPolicy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscordApiCachingTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void usesGuildCacheForRepeatedReads() throws Exception {
        AtomicInteger guildReads = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v10/guilds/1", exchange -> {
            guildReads.incrementAndGet();
            writeResponse(exchange, 200, "{\"id\":\"1\",\"name\":\"cached\"}");
        });
        server.start();

        DiscordApi api = newApiWithCache();
        api.getGuild("1");
        api.getGuild("1");

        assertEquals(1, guildReads.get());
    }

    @Test
    void invalidatesRoleCacheAfterMutation() throws Exception {
        AtomicInteger roleReads = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v10/guilds/1/roles", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                roleReads.incrementAndGet();
                writeResponse(exchange, 200, "[{\"id\":\"r1\"}]");
                return;
            }
            writeResponse(exchange, 200, "{\"id\":\"r2\"}");
        });
        server.start();

        DiscordApi api = newApiWithCache();

        api.listGuildRoles("1");
        api.listGuildRoles("1");
        api.createGuildRole("1", Map.of("name", "new-role"));
        api.listGuildRoles("1");

        assertEquals(2, roleReads.get());
    }

    private DiscordApi newApiWithCache() {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        DiscordClientConfig config = DiscordClientConfig.builder("token")
                .apiBaseUrl(baseUrl)
                .requestTimeout(Duration.ofSeconds(2))
                .build();

        DiscordRestClient restClient = new DiscordRestClient(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                config,
                new RetryPolicy(2, Duration.ofMillis(1), Duration.ofMillis(2), 0),
                RateLimitObserver.NOOP
        );

        return new DiscordApi(restClient, new DiscordStateCache());
    }

    private static void writeResponse(HttpExchange exchange, int code, String body) throws IOException {
        exchange.sendResponseHeaders(code, body.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
    }
}
