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
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class DiscordGatewayClient implements WebSocket.Listener, AutoCloseable {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DiscordClientConfig config;
    private final DiscordRestClient restClient;
    private final ExecutorService listenerExecutor;
    private final ScheduledExecutorService scheduler;

    private final Map<String, CopyOnWriteArrayList<Consumer<JsonNode>>> listeners = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<TypedEventListener<?>>> typedListeners = new ConcurrentHashMap<>();

    private final StringBuilder textBuffer = new StringBuilder();

    private volatile WebSocket webSocket;
    private volatile URI gatewayUri;
    private volatile URI resumeGatewayUri;
    private volatile String sessionId;
    private volatile long sequence = -1;
    private volatile boolean heartbeatAcked = true;
    private volatile boolean resumeOnReconnect = false;
    private volatile ScheduledFuture<?> heartbeatTask;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    public DiscordGatewayClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            DiscordClientConfig config,
            DiscordRestClient restClient
    ) {
        this(
                httpClient,
                objectMapper,
                config,
                restClient,
                Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("discord25-event-", 0).factory()),
                Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("discord25-heartbeat-", 0).factory())
        );
    }

    DiscordGatewayClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            DiscordClientConfig config,
            DiscordRestClient restClient,
            ExecutorService listenerExecutor,
            ScheduledExecutorService scheduler
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.config = config;
        this.restClient = restClient;
        this.listenerExecutor = Objects.requireNonNull(listenerExecutor, "listenerExecutor");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    public void connect() {
        GatewayBotInfo gatewayBotInfo = restClient.getGatewayBotInfo();
        this.gatewayUri = buildGatewayUri(gatewayBotInfo.url());
        this.webSocket = connect(gatewayUri);
    }

    public void on(String eventName, Consumer<JsonNode> listener) {
        listeners
                .computeIfAbsent(requireNonBlank(eventName, "eventName"), ignored -> new CopyOnWriteArrayList<>())
                .add(Objects.requireNonNull(listener, "listener"));
    }

    public <T> void on(String eventName, Class<T> eventType, Consumer<T> listener) {
        typedListeners
                .computeIfAbsent(requireNonBlank(eventName, "eventName"), ignored -> new CopyOnWriteArrayList<>())
                .add(TypedEventListener.withDefaultDeserializer(eventType, listener));
    }

    public <T> void on(
            String eventName,
            Class<T> eventType,
            EventDeserializer<T> deserializer,
            Consumer<T> listener
    ) {
        typedListeners
                .computeIfAbsent(requireNonBlank(eventName, "eventName"), ignored -> new CopyOnWriteArrayList<>())
                .add(new TypedEventListener<>(eventType, deserializer, listener));
    }

    public boolean off(String eventName, Consumer<JsonNode> listener) {
        CopyOnWriteArrayList<Consumer<JsonNode>> eventListeners = listeners.get(requireNonBlank(eventName, "eventName"));
        return eventListeners != null && eventListeners.remove(listener);
    }

    public <T> boolean off(String eventName, Class<T> eventType, Consumer<T> listener) {
        CopyOnWriteArrayList<TypedEventListener<?>> eventListeners =
                typedListeners.get(requireNonBlank(eventName, "eventName"));
        if (eventListeners == null) {
            return false;
        }

        return eventListeners.removeIf(existing -> existing.matches(eventType, listener));
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
            requestReconnect();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        requestReconnect();
    }

    private void handlePayload(WebSocket socket, JsonNode payload) {
        System.out.println("GW <= " + payload);

        int op = payload.path("op").asInt();
        JsonNode d = payload.path("d");
        JsonNode sequenceNode = payload.path("s");

        if (!sequenceNode.isMissingNode() && !sequenceNode.isNull()) {
            sequence = sequenceNode.asLong();
        }

        switch (op) {
            case 0 -> handleDispatch(payload.path("t").asText(), d);
            case 1 -> sendHeartbeat(socket);
            case 7 -> requestReconnect();
            case 9 -> {
                resumeOnReconnect = d.asBoolean(false);
                requestReconnect();
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
                dispatchAsync(() -> listener.accept(data));
            }
        }

        List<TypedEventListener<?>> typedEventListeners = typedListeners.get(eventType);
        if (typedEventListeners != null) {
            Map<TypedEventKey, Object> typedEventCache = new ConcurrentHashMap<>(typedEventListeners.size());
            for (TypedEventListener<?> typedListener : typedEventListeners) {
                dispatchAsync(() -> typedListener.accept(data, typedEventCache, objectMapper));
            }
        }
    }

    private void dispatchAsync(Runnable task) {
        listenerExecutor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException exception) {
                System.err.println("Gateway event listener failed: " + exception.getMessage());
            }
        });
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
                requestReconnect();
                return;
            }

            dispatchAsync(() -> sendHeartbeat(socket));
        }, initialDelay, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void requestReconnect() {
        if (reconnecting.compareAndSet(false, true)) {
            dispatchAsync(this::reconnect);
        }
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
            reconnecting.set(false);
            return;
        }

        try {
            webSocket = connect(target);
        } finally {
            reconnecting.set(false);
        }
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

        if (config.shardCount() > 1) {
            data.putArray("shard")
                    .add(config.shardId())
                    .add(config.shardCount());
        }

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

    @FunctionalInterface
    public interface EventDeserializer<T> {
        T deserialize(JsonNode rawEvent, ObjectMapper objectMapper);
    }

    private record TypedEventKey(Class<?> eventType, Object deserializerKey) {}

    private static final class TypedEventListener<T> {
        private final Class<T> eventType;
        private final EventDeserializer<T> deserializer;
        private final Consumer<T> listener;
        private final boolean customDeserializer;

        private static final Object DEFAULT_DESERIALIZER_KEY = new Object();

        private TypedEventListener(Class<T> eventType, EventDeserializer<T> deserializer, Consumer<T> listener) {
            this(eventType, deserializer, listener, true);
        }

        private TypedEventListener(
                Class<T> eventType,
                EventDeserializer<T> deserializer,
                Consumer<T> listener,
                boolean customDeserializer
        ) {
            this.eventType = Objects.requireNonNull(eventType, "eventType");
            this.deserializer = Objects.requireNonNull(deserializer, "deserializer");
            this.listener = Objects.requireNonNull(listener, "listener");
            this.customDeserializer = customDeserializer;
        }

        private static <T> EventDeserializer<T> defaultDeserializer(Class<T> eventType) {
            return (rawEvent, objectMapper) -> objectMapper.convertValue(rawEvent, eventType);
        }

        private static <T> TypedEventListener<T> withDefaultDeserializer(Class<T> eventType, Consumer<T> listener) {
            return new TypedEventListener<>(eventType, defaultDeserializer(eventType), listener, false);
        }

        @SuppressWarnings("unchecked")
        private void accept(JsonNode rawEvent, Map<TypedEventKey, Object> eventCache, ObjectMapper objectMapper) {
            Object deserializerKey = customDeserializer ? deserializer : DEFAULT_DESERIALIZER_KEY;
            TypedEventKey cacheKey = new TypedEventKey(eventType, deserializerKey);
            Object typedEvent = eventCache.computeIfAbsent(cacheKey, ignored -> deserialize(rawEvent, objectMapper));
            listener.accept((T) typedEvent);
        }

        private T deserialize(JsonNode rawEvent, ObjectMapper objectMapper) {
            try {
                return deserializer.deserialize(rawEvent, objectMapper);
            } catch (IllegalArgumentException exception) {
                throw new RuntimeException("Failed to deserialize gateway event to " + eventType.getName(), exception);
            }
        }

        private boolean matches(Class<?> expectedType, Consumer<?> expectedListener) {
            return eventType == expectedType && listener == expectedListener;
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public void close() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(true);
        }
        scheduler.shutdownNow();
        listenerExecutor.shutdownNow();

        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            } catch (Exception ignored) {
            }
        }
    }
}
