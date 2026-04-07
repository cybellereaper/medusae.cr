package com.github.cybellereaper.medusae.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.cybellereaper.medusae.client.DiscordClientConfig;
import com.github.cybellereaper.medusae.http.DiscordRestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class DiscordGatewayClient implements WebSocket.Listener, AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(DiscordGatewayClient.class.getName());
    private static final Duration RECONNECT_BASE_DELAY = Duration.ofSeconds(1);
    private static final Duration RECONNECT_MAX_DELAY = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DiscordClientConfig config;
    private final DiscordRestClient restClient;
    private final ExecutorService listenerExecutor;
    private final ScheduledExecutorService scheduler;

    private final Map<String, CopyOnWriteArrayList<Consumer<JsonNode>>> listeners = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<TypedEventListener<?>>> typedListeners = new ConcurrentHashMap<>();

    private final Object textBufferLock = new Object();
    private final StringBuilder textBuffer = new StringBuilder();
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private volatile WebSocket webSocket;
    private volatile URI gatewayUri;
    private volatile URI resumeGatewayUri;
    private volatile String sessionId;
    private volatile long sequence = -1;
    private volatile boolean heartbeatAcked = true;
    private volatile boolean resumeOnReconnect = false;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile ScheduledFuture<?> reconnectTask;

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

    private static boolean shouldReconnect(int statusCode) {
        return switch (statusCode) {
            case 4004, 4010, 4011, 4012, 4013, 4014 -> false;
            default -> true;
        };
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public void connect() {
        ensureOpen();
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
        try {
            return httpClient.newWebSocketBuilder()
                    .connectTimeout(config.requestTimeout())
                    .buildAsync(uri, this)
                    .join();
        } catch (CompletionException completionException) {
            throw new GatewayTransportException("Failed to connect to Discord gateway URI: " + uri, completionException);
        }
    }

    private URI buildGatewayUri(String baseUrl) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return URI.create(baseUrl + separator + "v=" + config.apiVersion() + "&encoding=json");
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        if (closed.get()) {
            webSocket.abort();
            return;
        }
        this.webSocket = webSocket;
        reconnectAttempts.set(0);
        LOGGER.log(System.Logger.Level.INFO, "Gateway connection opened");
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String payloadText = null;
        synchronized (textBufferLock) {
            textBuffer.append(data);

            if (last) {
                payloadText = textBuffer.toString();
                textBuffer.setLength(0);
            }
        }

        if (payloadText != null) {
            try {
                handlePayload(webSocket, readJson(payloadText));
            } catch (GatewayClientException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Failed to handle gateway payload: " + exception.getMessage(), exception);
                requestReconnect("payload handling failure");
            }
        }

        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.log(System.Logger.Level.WARNING, "Gateway closed (status={0}, reason={1})", statusCode, reason);
        cancelHeartbeat();

        if (!closed.get() && shouldReconnect(statusCode)) {
            requestReconnect("gateway close status " + statusCode);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOGGER.log(System.Logger.Level.WARNING, "Gateway transport error: " + error.getMessage(), error);
        if (!closed.get()) {
            requestReconnect("gateway transport error");
        }
    }

    private void handlePayload(WebSocket socket, JsonNode payload) {
        LOGGER.log(System.Logger.Level.DEBUG, "GW <= {0}", payload);

        int op = payload.path("op").asInt();
        JsonNode d = payload.path("d");
        JsonNode sequenceNode = payload.path("s");

        if (!sequenceNode.isMissingNode() && !sequenceNode.isNull()) {
            sequence = sequenceNode.asLong();
        }

        switch (op) {
            case 0 -> handleDispatch(payload.path("t").asText(), d);
            case 1 -> sendHeartbeat(socket);
            case 7 -> requestReconnect("reconnect requested by Discord gateway");
            case 9 -> {
                resumeOnReconnect = d.asBoolean(false);
                if (!resumeOnReconnect) {
                    clearResumableSessionState();
                }
                requestReconnect("invalid session");
            }
            case 10 -> {
                long intervalMillis = d.path("heartbeat_interval").asLong(-1);
                if (intervalMillis <= 0) {
                    throw new GatewayProtocolException("Missing or invalid heartbeat interval in HELLO payload");
                }
                startHeartbeat(intervalMillis);

                if (resumeOnReconnect && sessionId != null && resumeGatewayUri != null && sequence >= 0) {
                    sendResume(socket);
                } else {
                    sendIdentify(socket);
                }
            }
            case 11 -> heartbeatAcked = true;
            default -> LOGGER.log(System.Logger.Level.DEBUG, "Ignoring unsupported gateway op code: " + op);
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
                LOGGER.log(System.Logger.Level.WARNING, "Gateway event listener failed: " + exception.getMessage(), exception);
            }
        });
    }

    private void startHeartbeat(long intervalMillis) {
        cancelHeartbeat();
        heartbeatAcked = true;

        long initialDelay = ThreadLocalRandom.current().nextLong(Math.max(1, intervalMillis));

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            WebSocket socket = this.webSocket;
            if (socket == null) {
                return;
            }

            if (!heartbeatAcked) {
                LOGGER.log(System.Logger.Level.WARNING, "Heartbeat ACK missing; reconnecting");
                requestReconnect("heartbeat ACK timeout");
                return;
            }

            LOGGER.log(System.Logger.Level.DEBUG, "Sending gateway heartbeat");
            dispatchAsync(() -> sendHeartbeat(socket));
        }, initialDelay, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void requestReconnect(String reason) {
        if (closed.get()) {
            return;
        }
        if (reconnecting.compareAndSet(false, true)) {
            cancelHeartbeat();
            scheduleReconnect(reason);
        }
    }

    private void scheduleReconnect(String reason) {
        int attempt = reconnectAttempts.getAndIncrement();
        long rawDelayMillis = Math.min(
                RECONNECT_MAX_DELAY.toMillis(),
                RECONNECT_BASE_DELAY.toMillis() * (1L << Math.min(attempt, 5))
        );
        long jitterMillis = ThreadLocalRandom.current().nextLong(Math.max(1L, rawDelayMillis / 2L));
        long delayMillis = rawDelayMillis + jitterMillis;
        LOGGER.log(System.Logger.Level.INFO, "Scheduling gateway reconnect in {0}ms (reason={1}, attempt={2})",
                delayMillis, reason, attempt + 1);

        reconnectTask = scheduler.schedule(this::reconnect, delayMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void reconnect() {
        if (closed.get()) {
            reconnecting.set(false);
            return;
        }
        try {
            if (webSocket != null) {
                webSocket.abort();
            }
        } catch (Exception exception) {
            LOGGER.log(System.Logger.Level.DEBUG, "Ignoring WebSocket abort failure during reconnect", exception);
        }

        URI target = (resumeOnReconnect && resumeGatewayUri != null) ? resumeGatewayUri : gatewayUri;
        if (target == null) {
            LOGGER.log(System.Logger.Level.WARNING, "Reconnect requested before gateway URI initialization");
            reconnecting.set(false);
            return;
        }

        try {
            webSocket = connect(target);
            LOGGER.log(System.Logger.Level.INFO, "Gateway reconnect initiated");
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Gateway reconnect attempt failed: " + exception.getMessage(), exception);
            reconnecting.set(false);
            requestReconnect("reconnect attempt failed");
            return;
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
        socket.sendText(json, true)
                .exceptionally(throwable -> {
                    LOGGER.log(System.Logger.Level.WARNING, "Failed to send gateway payload op={0}: {1}", op, throwable.getMessage());
                    requestReconnect("gateway send failure");
                    return null;
                });
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException exception) {
            throw new GatewaySerializationException("Failed to parse gateway payload", exception);
        }
    }

    private String writeJson(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new GatewaySerializationException("Failed to serialize gateway payload", exception);
        }
    }

    private void clearResumableSessionState() {
        sessionId = null;
        resumeGatewayUri = null;
        sequence = -1;
    }

    private void cancelHeartbeat() {
        ScheduledFuture<?> currentHeartbeatTask = heartbeatTask;
        heartbeatTask = null;
        if (currentHeartbeatTask != null) {
            currentHeartbeatTask.cancel(true);
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Gateway client is already closed");
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        cancelHeartbeat();
        ScheduledFuture<?> currentReconnectTask = reconnectTask;
        reconnectTask = null;
        if (currentReconnectTask != null) {
            currentReconnectTask.cancel(true);
        }
        scheduler.shutdownNow();
        listenerExecutor.shutdownNow();

        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            } catch (Exception exception) {
                LOGGER.log(System.Logger.Level.DEBUG, "Ignoring WebSocket close failure", exception);
            }
        }
    }

    @FunctionalInterface
    public interface EventDeserializer<T> {
        T deserialize(JsonNode rawEvent, ObjectMapper objectMapper);
    }

    private record TypedEventKey(Class<?> eventType, Object deserializerKey) {
    }

    private record TypedEventListener<T>(Class<T> eventType, EventDeserializer<T> deserializer, Consumer<T> listener,
                                         boolean customDeserializer) {
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
                throw new GatewaySerializationException(
                        "Failed to deserialize gateway event to " + eventType.getName(),
                        exception
                );
            }
        }

        private boolean matches(Class<?> expectedType, Consumer<?> expectedListener) {
            return eventType == expectedType && listener == expectedListener;
        }
    }
}
