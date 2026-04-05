package com.github.cybellereaper.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.gateway.GatewayBotInfo;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscordRestClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DiscordClientConfig config;
    private final RateLimitManager rateLimitManager = new RateLimitManager();
    private final Map<String, String> routeToBucket = new ConcurrentHashMap<>();

    public DiscordRestClient(HttpClient httpClient, ObjectMapper objectMapper, DiscordClientConfig config) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    public GatewayBotInfo getGatewayBotInfo() {
        JsonNode node = request("GET", "/gateway/bot", null);
        JsonNode sessionStart = node.path("session_start_limit");

        return new GatewayBotInfo(
                node.path("url").asText(),
                node.path("shards").asInt(),
                new GatewayBotInfo.SessionStartLimit(
                        sessionStart.path("total").asInt(),
                        sessionStart.path("remaining").asInt(),
                        sessionStart.path("reset_after").asLong(),
                        sessionStart.path("max_concurrency").asInt()
                )
        );
    }

    public JsonNode sendMessage(String channelId, String content) {
        return request("POST", "/channels/" + channelId + "/messages", Map.of("content", content));
    }

    public JsonNode request(String method, String path, Object body) {
        String routeKey = method + " " + path;
        String bucketId = routeToBucket.getOrDefault(routeKey, routeKey);

        while (true) {
            rateLimitManager.await(bucketId);

            HttpRequest request = buildRequest(method, path, body);
            HttpResponse<String> response = send(request);

            String discoveredBucket = response.headers()
                    .firstValue("X-RateLimit-Bucket")
                    .orElse(null);

            if (discoveredBucket != null && !discoveredBucket.isBlank()) {
                routeToBucket.put(routeKey, discoveredBucket);
                bucketId = discoveredBucket;
            }

            rateLimitManager.updateFromHeaders(bucketId, response.headers());

            if (response.statusCode() == 429) {
                rateLimitManager.updateFrom429(bucketId, readJson(response.body()));
                continue;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new DiscordHttpException(response.statusCode(), response.body());
            }

            if (response.body() == null || response.body().isBlank()) {
                return objectMapper.createObjectNode();
            }

            return readJson(response.body());
        }
    }

    private HttpRequest buildRequest(String method, String path, Object body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(config.apiUri(path))
                .timeout(config.requestTimeout())
                .header("Authorization", "Bot " + config.botToken())
                .header("User-Agent", "discord25/0.1")
                .header("Accept", "application/json");

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
            return builder.build();
        }

        String json = writeJson(body);
        builder.header("Content-Type", "application/json");
        builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        return builder.build();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Discord API call interrupted", exception);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to call Discord API", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to serialize request body", exception);
        }
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to parse Discord response", exception);
        }
    }
}
