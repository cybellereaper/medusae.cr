package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.DiscordClientConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordRestClientRetryTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void retriesTransientHttpFailuresUsingBackoffPolicy() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v10/test", exchange -> {
            int attempt = requests.incrementAndGet();
            if (attempt < 3) {
                writeResponse(exchange, 502, "{\"error\":\"bad_gateway\"}");
                return;
            }
            writeResponse(exchange, 200, "{\"ok\":true}");
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        DiscordClientConfig config = DiscordClientConfig.builder("token")
                .apiBaseUrl(baseUrl)
                .requestTimeout(Duration.ofSeconds(2))
                .build();

        DiscordRestClient restClient = new DiscordRestClient(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                config,
                new RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(2), 0),
                RateLimitObserver.NOOP
        );

        assertTrue(restClient.request("GET", "/test", null).path("ok").asBoolean());
        assertEquals(3, requests.get());
    }

    private static void writeResponse(HttpExchange exchange, int code, String body) throws IOException {
        exchange.sendResponseHeaders(code, body.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
    }
}
