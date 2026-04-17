package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.DiscordAttachment;
import com.github.cybellereaper.medusae.client.DiscordClientConfig;
import com.github.cybellereaper.medusae.client.SlashCommandDefinition;
import com.github.cybellereaper.medusae.gateway.GatewayBotInfo;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscordRestClient {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DiscordClientConfig config;
    private final RetryPolicy retryPolicy;
    private final RateLimitObserver rateLimitObserver;
    private final RateLimitManager rateLimitManager;
    private final Map<String, String> routeToBucket = new ConcurrentHashMap<>();

    public DiscordRestClient(HttpClient httpClient, ObjectMapper objectMapper, DiscordClientConfig config) {
        this(httpClient, objectMapper, config, RetryPolicy.defaultPolicy(), RateLimitObserver.NOOP);
    }

    public DiscordRestClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            DiscordClientConfig config,
            RetryPolicy retryPolicy,
            RateLimitObserver rateLimitObserver
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.config = Objects.requireNonNull(config, "config");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.rateLimitObserver = Objects.requireNonNull(rateLimitObserver, "rateLimitObserver");
        this.rateLimitManager = new RateLimitManager(rateLimitObserver);
    }

    private static void validateCommandContext(String applicationId, SlashCommandDefinition command) {
        requireNonBlank(applicationId, "applicationId");
        Objects.requireNonNull(command, "command");
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
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

    public JsonNode getCurrentApplication() {
        return request("GET", "/oauth2/applications/@me", null);
    }

    public String getCurrentApplicationId() {
        return getCurrentApplication().path("id").asText();
    }

    public JsonNode sendMessage(String channelId, Map<String, Object> payload) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(payload, "payload");
        return request("POST", "/channels/" + channelId + "/messages", payload);
    }

    public JsonNode sendMessageWithAttachments(String channelId, Map<String, Object> payload, List<DiscordAttachment> attachments) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(attachments, "attachments");

        String path = "/channels/" + channelId + "/messages";
        if (attachments.isEmpty()) {
            return request("POST", path, payload);
        }

        MultipartRequestBody multipartBody = new MultipartRequestBody()
                .addJsonPart("payload_json", writeJson(payload));

        for (int i = 0; i < attachments.size(); i++) {
            DiscordAttachment attachment = attachments.get(i);
            multipartBody.addFilePart("files[" + i + "]", attachment.fileName(), attachment.contentType(), attachment.content());
        }

        HttpRequest.Builder builder = baseRequestBuilder(path)
                .header("Content-Type", "multipart/form-data; boundary=" + multipartBody.boundary())
                .method("POST", multipartBody.toPublisher());

        return requestInternal("POST", path, builder.build());
    }

    public JsonNode getMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return request("GET", "/channels/" + channelId + "/messages/" + messageId, null);
    }

    public JsonNode editMessage(String channelId, String messageId, Map<String, Object> payload) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        Objects.requireNonNull(payload, "payload");
        return request("PATCH", "/channels/" + channelId + "/messages/" + messageId, payload);
    }

    public JsonNode getChannelMessages(String channelId, Map<String, Object> query) {
        requireNonBlank(channelId, "channelId");
        Objects.requireNonNull(query, "query");
        return request("GET", "/channels/" + channelId + "/messages" + toQueryString(query), null);
    }

    public JsonNode addReaction(String channelId, String messageId, String emoji) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        return request("PUT", "/channels/" + channelId + "/messages/" + messageId + "/reactions/" + emoji + "/@me", null);
    }

    public JsonNode removeOwnReaction(String channelId, String messageId, String emoji) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        return request("DELETE", "/channels/" + channelId + "/messages/" + messageId + "/reactions/" + emoji + "/@me", null);
    }

    public JsonNode removeUserReaction(String channelId, String messageId, String emoji, String userId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        requireNonBlank(userId, "userId");
        return request("DELETE", "/channels/" + channelId + "/messages/" + messageId + "/reactions/" + emoji + "/" + userId, null);
    }

    public JsonNode clearMessageReactionEmoji(String channelId, String messageId, String emoji) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        requireNonBlank(emoji, "emoji");
        return request("DELETE", "/channels/" + channelId + "/messages/" + messageId + "/reactions/" + emoji, null);
    }

    public JsonNode clearMessageReactions(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return request("DELETE", "/channels/" + channelId + "/messages/" + messageId + "/reactions", null);
    }

    public JsonNode pinMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return request("PUT", "/channels/" + channelId + "/pins/" + messageId, null);
    }

    public JsonNode unpinMessage(String channelId, String messageId) {
        requireNonBlank(channelId, "channelId");
        requireNonBlank(messageId, "messageId");
        return request("DELETE", "/channels/" + channelId + "/pins/" + messageId, null);
    }

    public JsonNode listPinnedMessages(String channelId) {
        requireNonBlank(channelId, "channelId");
        return request("GET", "/channels/" + channelId + "/pins", null);
    }

    public JsonNode triggerTypingIndicator(String channelId) {
        requireNonBlank(channelId, "channelId");
        return request("POST", "/channels/" + channelId + "/typing", null);
    }

    public JsonNode createGuildApplicationCommand(String applicationId, String guildId, SlashCommandDefinition command) {
        validateCommandContext(applicationId, command);
        requireNonBlank(guildId, "guildId");

        return request("POST", "/applications/" + applicationId + "/guilds/" + guildId + "/commands", command.toRequestPayload());
    }

    public JsonNode createInteractionResponse(String interactionId, String interactionToken, int type, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", type);
        if (data != null && !data.isEmpty()) {
            payload.put("data", data);
        }

        return request("POST", "/interactions/" + interactionId + "/" + interactionToken + "/callback", payload);
    }

    public JsonNode createGlobalApplicationCommand(String applicationId, SlashCommandDefinition command) {
        validateCommandContext(applicationId, command);
        return request("POST", "/applications/" + applicationId + "/commands", command.toRequestPayload());
    }

    public JsonNode request(String method, String path, Object body) {
        HttpRequest request = buildRequest(method, path, body);
        return requestInternal(method, path, request);
    }

    private JsonNode requestInternal(String method, String path, HttpRequest request) {
        String routeKey = method + " " + path;
        String bucketId = routeToBucket.getOrDefault(routeKey, routeKey);
        Instant startedAt = Instant.now();

        for (int attempt = 1; attempt <= retryPolicy.maxAttempts(); attempt++) {
            rateLimitManager.await(bucketId);
            HttpResponse<String> response;

            try {
                response = send(request);
            } catch (RuntimeException exception) {
                if (handleTransportFailure(method, path, attempt, exception)) {
                    continue;
                }
                throw exception;
            }

            bucketId = updateRateLimitBucket(routeKey, bucketId, response);

            rateLimitManager.updateFromHeaders(bucketId, response.headers());

            if (handleRateLimit429(method, path, bucketId, attempt, response)) {
                continue;
            }

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (shouldRetryHttpStatus(response.statusCode(), attempt)) {
                    Duration backoff = retryPolicy.delayForAttempt(attempt);
                    rateLimitObserver.onRetryScheduled(method, path, attempt, backoff, "http_" + response.statusCode());
                    sleep(backoff);
                    continue;
                }

                recordCompletion(method, path, attempt, response.statusCode(), startedAt);
                throw new DiscordHttpException(response.statusCode(), response.body());
            }

            recordCompletion(method, path, attempt, response.statusCode(), startedAt);

            if (response.body() == null || response.body().isBlank()) {
                return objectMapper.createObjectNode();
            }

            return readJson(response.body());
        }

        throw new IllegalStateException("Request attempts exhausted unexpectedly");
    }

    private boolean handleTransportFailure(String method, String path, int attempt, RuntimeException exception) {
        if (attempt == retryPolicy.maxAttempts()) {
            return false;
        }
        Duration backoff = retryPolicy.delayForAttempt(attempt);
        rateLimitObserver.onRetryScheduled(method, path, attempt, backoff, "transport");
        sleep(backoff);
        return true;
    }

    private String updateRateLimitBucket(String routeKey, String currentBucketId, HttpResponse<String> response) {
        String discoveredBucket = response.headers()
                .firstValue("X-RateLimit-Bucket")
                .orElse(null);

        if (discoveredBucket == null || discoveredBucket.isBlank()) {
            return currentBucketId;
        }

        routeToBucket.put(routeKey, discoveredBucket);
        return discoveredBucket;
    }

    private boolean shouldRetryHttpStatus(int statusCode, int attempt) {
        return retryPolicy.shouldRetryStatus(statusCode) && attempt < retryPolicy.maxAttempts();
    }

    private boolean handleRateLimit429(String method, String path, String bucketId, int attempt, HttpResponse<String> response) {
        if (response.statusCode() != 429) {
            return false;
        }

        JsonNode body = readJsonOrEmpty(response.body());
        double retryAfterSeconds = rateLimitManager.updateFrom429(bucketId, body);
        if (attempt >= retryPolicy.maxAttempts()) {
            return false;
        }

        Duration backoff = Duration.ofMillis(Math.max(1L, Math.round(retryAfterSeconds * 1000)));
        rateLimitObserver.onRetryScheduled(method, path, attempt, backoff, "rate_limit");
        sleep(backoff);
        return true;
    }

    private void recordCompletion(String method, String path, int attempt, int statusCode, Instant startedAt) {
        rateLimitObserver.onRequestCompleted(
                method,
                path,
                attempt,
                statusCode,
                Duration.between(startedAt, Instant.now())
        );
    }

    private HttpRequest buildRequest(String method, String path, Object body) {
        HttpRequest.Builder builder = baseRequestBuilder(path);

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
            return builder.build();
        }

        String json = writeJson(body);
        builder.header("Content-Type", "application/json");
        builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        return builder.build();
    }

    private HttpRequest.Builder baseRequestBuilder(String path) {
        return HttpRequest.newBuilder(config.apiUri(path))
                .timeout(config.requestTimeout())
                .header("Authorization", "Bot " + config.botToken())
                .header("User-Agent", "discord25/0.1")
                .header("Accept", "application/json");
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

    private void sleep(Duration duration) {
        try {
            Thread.sleep(Math.max(1, duration.toMillis()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry sleep interrupted", exception);
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

    private JsonNode readJsonOrEmpty(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return readJson(json);
    }

    private String toQueryString(Map<String, Object> query) {
        if (query.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key == null || key.isBlank() || value == null) {
                continue;
            }
            if (!first) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8));
            first = false;
        }
        return first ? "" : builder.toString();
    }
}
