package com.github.cybellereaper.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.gateway.events.MessageCreateEvent;
import com.github.cybellereaper.gateway.events.ReadyEvent;
import com.github.cybellereaper.http.DiscordRestClient;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DiscordGatewayClientTest {
    @Test
    void dispatchesTypedEventsUsingRegisteredType() {
        CountingObjectMapper mapper = new CountingObjectMapper();
        DiscordGatewayClient client = gatewayClient(mapper);
        try {
            AtomicReference<TestEvent> first = new AtomicReference<>();
            AtomicReference<TestEvent> second = new AtomicReference<>();

            client.on("TEST_EVENT", TestEvent.class, first::set);
            client.on("TEST_EVENT", TestEvent.class, second::set);

            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"TEST_EVENT","d":{"id":"42","name":"typed"}}
                    """, true);

            assertEquals("42", first.get().id());
            assertEquals("typed", second.get().name());
            assertEquals(1, mapper.convertCalls.get(), "typed conversion should be cached per class");
        } finally {
            client.close();
        }
    }

    @Test
    void dispatchesRawAndTypedListenersForSameEvent() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
        try {
            AtomicReference<JsonNode> rawPayload = new AtomicReference<>();
            AtomicReference<TestEvent> typedPayload = new AtomicReference<>();

            client.on("TEST_EVENT", rawPayload::set);
            client.on("TEST_EVENT", TestEvent.class, typedPayload::set);

            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"TEST_EVENT","d":{"id":"7","name":"hello"}}
                    """, true);

            assertEquals("7", rawPayload.get().path("id").asText());
            assertEquals("hello", typedPayload.get().name());
        } finally {
            client.close();
        }
    }

    @Test
    void supportsCustomDeserializerAndCachesResultAcrossListeners() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
        try {
            AtomicInteger deserializeCalls = new AtomicInteger(0);
            AtomicReferenceArray<String> values = new AtomicReferenceArray<>(2);

            DiscordGatewayClient.EventDeserializer<String> trimmedContentDeserializer = (rawEvent, ignoredMapper) -> {
                deserializeCalls.incrementAndGet();
                return rawEvent.path("content").asText().trim();
            };

            client.on("MESSAGE_CREATE", String.class, trimmedContentDeserializer, value -> values.set(0, value));
            client.on("MESSAGE_CREATE", String.class, trimmedContentDeserializer, value -> values.set(1, value));

            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"MESSAGE_CREATE","d":{"content":"  hello  "}}
                    """, true);

            assertEquals("hello", values.get(0));
            assertEquals("hello", values.get(1));
            assertEquals(1, deserializeCalls.get());
        } finally {
            client.close();
        }
    }

    @Test
    void canUnsubscribeRawAndTypedListeners() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
        try {
            AtomicInteger rawInvocations = new AtomicInteger(0);
            AtomicInteger typedInvocations = new AtomicInteger(0);

            java.util.function.Consumer<JsonNode> rawListener = ignored -> rawInvocations.incrementAndGet();
            java.util.function.Consumer<TestEvent> typedListener = ignored -> typedInvocations.incrementAndGet();

            client.on("TEST_EVENT", rawListener);
            client.on("TEST_EVENT", TestEvent.class, typedListener);
            assertTrue(client.off("TEST_EVENT", rawListener));
            assertTrue(client.off("TEST_EVENT", TestEvent.class, typedListener));

            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"TEST_EVENT","d":{"id":"1","name":"n"}}
                    """, true);

            assertEquals(0, rawInvocations.get());
            assertEquals(0, typedInvocations.get());
        } finally {
            client.close();
        }
    }

    @Test
    void mapsReadyAndMessageCreateEventsToBuiltInTypedModels() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
        try {
            AtomicReference<ReadyEvent> readyEventRef = new AtomicReference<>();
            AtomicReference<MessageCreateEvent> messageEventRef = new AtomicReference<>();

            client.on("READY", ReadyEvent.class, readyEventRef::set);
            client.on("MESSAGE_CREATE", MessageCreateEvent.class, messageEventRef::set);

            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"READY","d":{"v":10,"session_id":"abc","resume_gateway_url":"wss://gateway.discord.gg","user":{"id":"1","username":"bot"}}}
                    """, true);
            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"MESSAGE_CREATE","d":{"id":"m1","channel_id":"c1","guild_id":"g1","content":"Hello","author":{"id":"u1","username":"neo","discriminator":"0001"}}}
                    """, true);

            assertEquals("abc", readyEventRef.get().sessionId());
            assertEquals("wss://gateway.discord.gg", readyEventRef.get().resumeGatewayUrl());
            assertEquals("m1", messageEventRef.get().id());
            assertEquals("neo", messageEventRef.get().author().username());
        } finally {
            client.close();
        }
    }

    @Test
    void identifyPayloadIncludesShardWhenConfigured() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        DiscordClientConfig config = DiscordClientConfig.builder("token")
                .intents(513)
                .shard(1, 4)
                .build();
        DiscordGatewayClient client = new DiscordGatewayClient(
                HttpClient.newHttpClient(),
                mapper,
                config,
                new DiscordRestClient(HttpClient.newHttpClient(), mapper, config)
        );
        try {
            CapturingWebSocket socket = new CapturingWebSocket();
            client.onOpen(socket);
            client.onText(socket, "{\"op\":10,\"d\":{\"heartbeat_interval\":45000}}", true);

            JsonNode sentPayload = mapper.readTree(socket.lastSentText.get());
            assertEquals(2, sentPayload.path("op").asInt());
            assertEquals(1, sentPayload.path("d").path("shard").get(0).asInt());
            assertEquals(4, sentPayload.path("d").path("shard").get(1).asInt());
        } finally {
            client.close();
        }
    }

    @Test
    void defaultConfigurationDispatchesListenersOnVirtualThreads() throws InterruptedException {
        DiscordGatewayClient client = gatewayClientWithVirtualThreadDispatch(new ObjectMapper());
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Boolean> ranOnVirtualThread = new AtomicReference<>(false);

            client.on("TEST_EVENT", ignored -> {
                ranOnVirtualThread.set(Thread.currentThread().isVirtual());
                latch.countDown();
            });

            client.onText(new StubWebSocket(), """
                    {"op":0,"t":"TEST_EVENT","d":{"id":"42","name":"typed"}}
                    """, true);

            assertTrue(latch.await(2, TimeUnit.SECONDS), "listener should run asynchronously");
            assertEquals(Boolean.TRUE, ranOnVirtualThread.get());
        } finally {
            client.close();
        }
    }

    @Test
    void helloSendsResumeWhenSessionStateExists() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        DiscordGatewayClient client = gatewayClient(mapper, scheduler);
        try {
            CapturingWebSocket socket = new CapturingWebSocket();
            client.onText(socket, """
                    {"op":0,"t":"READY","s":3,"d":{"session_id":"abc","resume_gateway_url":"wss://gateway.discord.gg"}}
                    """, true);
            client.onText(socket, "{\"op\":10,\"d\":{\"heartbeat_interval\":45000}}", true);

            JsonNode payload = mapper.readTree(socket.lastSentText.get());
            assertEquals(6, payload.path("op").asInt());
            assertEquals("abc", payload.path("d").path("session_id").asText());
            assertEquals(3, payload.path("d").path("seq").asInt());
            assertNotNull(scheduler.fixedRateTask, "heartbeat task should be scheduled from HELLO");
        } finally {
            client.close();
        }
    }

    @Test
    void invalidSessionWithoutResumeForcesIdentifyOnNextHello() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        DiscordGatewayClient client = gatewayClient(mapper, scheduler);
        try {
            CapturingWebSocket socket = new CapturingWebSocket();
            client.onText(socket, """
                    {"op":0,"t":"READY","s":10,"d":{"session_id":"abc","resume_gateway_url":"wss://gateway.discord.gg"}}
                    """, true);
            client.onText(socket, "{\"op\":9,\"d\":false}", true);
            client.onText(socket, "{\"op\":10,\"d\":{\"heartbeat_interval\":45000}}", true);

            JsonNode payload = mapper.readTree(socket.lastSentText.get());
            assertEquals(2, payload.path("op").asInt(), "invalid non-resumable session should trigger IDENTIFY");
        } finally {
            client.close();
        }
    }

    @Test
    void missingHeartbeatAckTriggersReconnectInsteadOfSendingAnotherHeartbeat() {
        ObjectMapper mapper = new ObjectMapper();
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        DiscordGatewayClient client = gatewayClient(mapper, scheduler);
        try {
            CapturingWebSocket socket = new CapturingWebSocket();
            client.onOpen(socket);
            client.onText(socket, "{\"op\":10,\"d\":{\"heartbeat_interval\":45000}}", true);

            assertNotNull(scheduler.fixedRateTask);
            int baselineMessages = socket.sentCount.get();
            setHeartbeatAcked(client, false);

            scheduler.fixedRateTask.run();
            assertEquals(baselineMessages, socket.sentCount.get(), "heartbeat should not send without ACK");
            assertNotNull(scheduler.scheduledTask, "reconnect should be scheduled after missed ACK");
        } finally {
            client.close();
        }
    }

    @Test
    void malformedJsonPayloadSchedulesReconnect() {
        ObjectMapper mapper = new ObjectMapper();
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        DiscordGatewayClient client = gatewayClient(mapper, scheduler);
        try {
            CapturingWebSocket socket = new CapturingWebSocket();

            assertDoesNotThrow(() -> client.onText(socket, "{\"op\":", true));
            assertNotNull(scheduler.scheduledTask, "malformed payload should trigger reconnect scheduling");
        } finally {
            client.close();
        }
    }

    @Test
    void helloWithoutHeartbeatIntervalSchedulesReconnect() {
        ObjectMapper mapper = new ObjectMapper();
        ManualScheduledExecutor scheduler = new ManualScheduledExecutor();
        DiscordGatewayClient client = gatewayClient(mapper, scheduler);
        try {
            CapturingWebSocket socket = new CapturingWebSocket();

            assertDoesNotThrow(() -> client.onText(socket, "{\"op\":10,\"d\":{}}", true));
            assertNotNull(scheduler.scheduledTask, "invalid HELLO payload should trigger reconnect scheduling");
        } finally {
            client.close();
        }
    }

    private static void setHeartbeatAcked(DiscordGatewayClient client, boolean acked) {
        try {
            var field = DiscordGatewayClient.class.getDeclaredField("heartbeatAcked");
            field.setAccessible(true);
            field.set(client, acked);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to mutate heartbeatAcked in test setup", exception);
        }
    }

    private static DiscordGatewayClient gatewayClient(ObjectMapper mapper) {
        return gatewayClient(mapper, Executors.newSingleThreadScheduledExecutor());
    }

    private static DiscordGatewayClient gatewayClient(ObjectMapper mapper, ScheduledExecutorService scheduler) {
        HttpClient httpClient = HttpClient.newHttpClient();
        DiscordClientConfig config = DiscordClientConfig.builder("token")
                .intents(0)
                .apiBaseUrl("https://discord.com/api")
                .apiVersion(10)
                .requestTimeout(Duration.ofSeconds(5))
                .build();
        DiscordRestClient restClient = new DiscordRestClient(httpClient, mapper, config);
        return new DiscordGatewayClient(
                httpClient,
                mapper,
                config,
                restClient,
                new DirectExecutorService(),
                scheduler
        );
    }

    private static DiscordGatewayClient gatewayClientWithVirtualThreadDispatch(ObjectMapper mapper) {
        HttpClient httpClient = HttpClient.newHttpClient();
        DiscordClientConfig config = DiscordClientConfig.builder("token")
                .intents(0)
                .apiBaseUrl("https://discord.com/api")
                .apiVersion(10)
                .requestTimeout(Duration.ofSeconds(5))
                .build();
        DiscordRestClient restClient = new DiscordRestClient(httpClient, mapper, config);
        return new DiscordGatewayClient(httpClient, mapper, config, restClient);
    }

    private record TestEvent(String id, String name) {
    }

    private static final class CountingObjectMapper extends ObjectMapper {
        private final AtomicInteger convertCalls = new AtomicInteger();

        @Override
        public <T> T convertValue(Object fromValue, Class<T> toValueType) throws IllegalArgumentException {
            if (toValueType == TestEvent.class) {
                convertCalls.incrementAndGet();
            }
            return super.convertValue(fromValue, toValueType);
        }
    }

    private static class StubWebSocket implements WebSocket {
        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
        }

        @Override
        public String getSubprotocol() {
            return null;
        }

        @Override
        public boolean isOutputClosed() {
            return false;
        }

        @Override
        public boolean isInputClosed() {
            return false;
        }

        @Override
        public void abort() {
        }
    }

    private static final class CapturingWebSocket extends StubWebSocket {
        private final AtomicReference<String> lastSentText = new AtomicReference<>();
        private final AtomicInteger sentCount = new AtomicInteger();

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            sentCount.incrementAndGet();
            lastSentText.set(data.toString());
            return CompletableFuture.completedFuture(this);
        }
    }

    private static final class ManualScheduledExecutor extends AbstractExecutorService implements ScheduledExecutorService {
        private volatile boolean shutdown;
        private Runnable fixedRateTask;
        private Runnable scheduledTask;

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduledTask = command;
            return new CompletedScheduledFuture();
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            fixedRateTask = command;
            return new CompletedScheduledFuture();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class CompletedScheduledFuture implements ScheduledFuture<Object> {
        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {
        private volatile boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
