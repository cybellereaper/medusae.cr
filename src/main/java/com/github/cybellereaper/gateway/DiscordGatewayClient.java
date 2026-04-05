package com.github.cybellereaper.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.http.DiscordRestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import java.util.function.Consumer;

public final class DiscordGatewayClient implements WebSocket.Listener, AutoCloseable {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DiscordClientConfig config;
    private final DiscordRestClient restClient;

    private final Map<String, CopyOnWriteArrayList<Consumer<JsonNode>>> listeners = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("discord25-heartbeat-", 0).factory());

    private final StringBuilder textBuffer = new StringBuilder();

    private volatile WebSocket webSocket;
    private volatile URI gatewayUri;
    private volatile URI resumeGatewayUri;
    private volatile String sessionId;
    private volatile long sequence = -1;
    private volatile boolean heartbeatAcked = true;
    private volatile boolean resumeOnReconnect = false;
    private volatile ScheduledFuture<?> heartbeatTask;

    public DiscordGatewayClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            DiscordClientConfig config,
            DiscordRestClient restClient
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.config = config;
        this.restClient = restClient;
    }

    public void connect() {
        GatewayBotInfo gatewayBotInfo = restClient.getGatewayBotInfo();
        this.gatewayUri = buildGatewayUri(gatewayBotInfo.url());
        this.webSocket = connect(gatewayUri);
    }

    public void on(String eventName, Consumer<JsonNode> listener) {
        listeners.computeIfAbsent(eventName, ignored -> new CopyOnWriteArrayList<>()).add(listener);
    }

    private WebSocket connect(URI uri) {
        return httpClient.newWebSocketBuilder()
                .connectTimeout(config.requestTimeout())
                .buildAsync(uri, this)
                .join();
    }

    private URI buildGatewayUri(String baseUrl) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return URI.create(baseUrl + separator + "v=" + config.apiVersion() + "&encoding=json");
    }



    @Override
    public void onOpen(WebSocket webSocket) {
        this.webSocket = webSocket;
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        textBuffer.append(data);

        if (last) {
            String payloadText = textBuffer.toString();
            textBuffer.setLength(0);
            handlePayload(webSocket, readJson(payloadText));
        }

        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    private static boolean shouldReconnect(int statusCode) {
        return switch (statusCode) {
            case 4004, 4010, 4011, 4012, 4013, 4014 -> false;
            default -> true;
        };
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        System.err.println("Gateway closed: " + statusCode + " - " + reason);

        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }

        if (shouldReconnect(statusCode)) {
            reconnect();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        reconnect();
    }

    private void handlePayload(WebSocket socket, JsonNode payload) {
        System.out.println("GW <= " + payload);

        int op = payload.path("op").asInt();
        JsonNode d = payload.path("d");

        if (!payload.path("s").isMissingNode() && !payload.path("s").isNull()) {
            sequence = payload.path("s").asLong();
        }

        switch (op) {
            case 0 -> handleDispatch(payload.path("t").asText(), d);
            case 1 -> sendHeartbeat(socket);
            case 7 -> reconnect();
            case 9 -> {
                resumeOnReconnect = d.asBoolean(false);
                reconnect();
            }
            case 10 -> {
                long intervalMillis = d.path("heartbeat_interval").asLong();
                startHeartbeat(intervalMillis);

                if (resumeOnReconnect && sessionId != null && resumeGatewayUri != null && sequence >= 0) {
                    sendResume(socket);
                } else {
                    sendIdentify(socket);
                }
            }
            case 11 -> heartbeatAcked = true;
            default -> { }
        }
    }

    private void handleDispatch(String eventType, JsonNode data) {
        if ("READY".equals(eventType)) {
            sessionId = data.path("session_id").asText(null);

            String resumeGatewayUrl = data.path("resume_gateway_url").asText(null);
            if (resumeGatewayUrl != null && !resumeGatewayUrl.isBlank()) {
                resumeGatewayUri = buildGatewayUri(resumeGatewayUrl);
            }

            resumeOnReconnect = true;
        }

        List<Consumer<JsonNode>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            for (Consumer<JsonNode> listener : eventListeners) {
                listener.accept(data);
            }
        }
    }

    private void startHeartbeat(long intervalMillis) {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }

        long initialDelay = ThreadLocalRandom.current().nextLong(Math.max(1, intervalMillis));

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            WebSocket socket = this.webSocket;
            if (socket == null) {
                return;
            }

            if (!heartbeatAcked) {
                reconnect();
                return;
            }

            sendHeartbeat(socket);
        }, initialDelay, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void reconnect() {
        try {
            if (webSocket != null) {
                webSocket.abort();
            }
        } catch (Exception ignored) {
        }

        URI target = (resumeOnReconnect && resumeGatewayUri != null) ? resumeGatewayUri : gatewayUri;
        if (target == null) {
            return;
        }

        webSocket = connect(target);
    }

    private void sendIdentify(WebSocket socket) {
        ObjectNode properties = objectMapper.createObjectNode()
                .put("os", System.getProperty("os.name"))
                .put("browser", "discord25")
                .put("device", "discord25");

        ObjectNode data = objectMapper.createObjectNode()
                .put("token", config.botToken())
                .put("intents", config.intents())
                .set("properties", properties);

        send(socket, 2, data);
    }

    private void sendResume(WebSocket socket) {
        ObjectNode data = objectMapper.createObjectNode()
                .put("token", config.botToken())
                .put("session_id", sessionId)
                .put("seq", sequence);

        send(socket, 6, data);
    }

    private void sendHeartbeat(WebSocket socket) {
        heartbeatAcked = false;

        if (sequence >= 0) {
            send(socket, 1, objectMapper.getNodeFactory().numberNode(sequence));
        } else {
            send(socket, 1, objectMapper.nullNode());
        }
    }

    private void send(WebSocket socket, int op, JsonNode d) {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("op", op)
                .set("d", d);

        String json = writeJson(payload);
        System.out.println("GW => " + json);
        socket.sendText(json, true);
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to parse gateway payload", exception);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to serialize gateway payload", exception);
        }
    }

    @Override
    public void close() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }
        scheduler.shutdownNow();

        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            } catch (Exception ignored) {
            }
        }
    }
}
