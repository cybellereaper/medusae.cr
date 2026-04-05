package com.github.cybellereaper.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.gateway.events.GuildCreateEvent;
import com.github.cybellereaper.gateway.events.InteractionCreateEvent;
import com.github.cybellereaper.gateway.events.MessageCreateEvent;
import com.github.cybellereaper.gateway.events.MessageDeleteEvent;
import com.github.cybellereaper.gateway.events.ReadyEvent;
import com.github.cybellereaper.http.DiscordRestClient;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DiscordGatewayClientTest {
    @Test
    void dispatchesTypedEventsUsingRegisteredType() {
        CountingObjectMapper mapper = new CountingObjectMapper();
        DiscordGatewayClient client = gatewayClient(mapper);

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
    }

    @Test
    void dispatchesRawAndTypedListenersForSameEvent() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());

        AtomicReference<JsonNode> rawPayload = new AtomicReference<>();
        AtomicReference<TestEvent> typedPayload = new AtomicReference<>();

        client.on("TEST_EVENT", rawPayload::set);
        client.on("TEST_EVENT", TestEvent.class, typedPayload::set);

        client.onText(new StubWebSocket(), """
                {"op":0,"t":"TEST_EVENT","d":{"id":"7","name":"hello"}}
                """, true);

        assertEquals("7", rawPayload.get().path("id").asText());
        assertEquals("hello", typedPayload.get().name());
    }

    @Test
    void supportsCustomDeserializerAndCachesResultAcrossListeners() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
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
    }

    @Test
    void canUnsubscribeRawAndTypedListeners() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
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
    }

    @Test
    void mapsBuiltInTypedGatewayEvents() {
        DiscordGatewayClient client = gatewayClient(new ObjectMapper());
        AtomicReference<ReadyEvent> readyEventRef = new AtomicReference<>();
        AtomicReference<MessageCreateEvent> messageCreateEventRef = new AtomicReference<>();
        AtomicReference<MessageDeleteEvent> messageDeleteEventRef = new AtomicReference<>();
        AtomicReference<GuildCreateEvent> guildCreateEventRef = new AtomicReference<>();
        AtomicReference<InteractionCreateEvent> interactionCreateEventRef = new AtomicReference<>();

        client.on("READY", ReadyEvent.class, readyEventRef::set);
        client.on("MESSAGE_CREATE", MessageCreateEvent.class, messageCreateEventRef::set);
        client.on("MESSAGE_DELETE", MessageDeleteEvent.class, messageDeleteEventRef::set);
        client.on("GUILD_CREATE", GuildCreateEvent.class, guildCreateEventRef::set);
        client.on("INTERACTION_CREATE", InteractionCreateEvent.class, interactionCreateEventRef::set);

        client.onText(new StubWebSocket(), """
                {"op":0,"t":"READY","d":{"session_id":"abc","resume_gateway_url":"wss://gateway.discord.gg"}}
                """, true);
        client.onText(new StubWebSocket(), """
                {"op":0,"t":"MESSAGE_CREATE","d":{"id":"m1","channel_id":"c1","guild_id":"g1","content":"Hello","author":{"id":"u1","username":"neo","discriminator":"0001"}}}
                """, true);
        client.onText(new StubWebSocket(), """
                {"op":0,"t":"MESSAGE_DELETE","d":{"id":"m1","channel_id":"c1","guild_id":"g1"}}
                """, true);
        client.onText(new StubWebSocket(), """
                {"op":0,"t":"GUILD_CREATE","d":{"id":"g1","name":"Jellycord","member_count":42,"unavailable":false}}
                """, true);
        client.onText(new StubWebSocket(), """
                {"op":0,"t":"INTERACTION_CREATE","d":{"id":"i1","type":2,"token":"token-1","guild_id":"g1","channel_id":"c1","data":{"id":"cmd1","name":"ping"},"member":{"nick":"cybelle","user":{"id":"u1","username":"neo","discriminator":"0001"}}}}
                """, true);

        assertEquals("abc", readyEventRef.get().sessionId());
        assertEquals("wss://gateway.discord.gg", readyEventRef.get().resumeGatewayUrl());

        assertEquals("m1", messageCreateEventRef.get().id());
        assertEquals("neo", messageCreateEventRef.get().author().username());

        assertEquals("m1", messageDeleteEventRef.get().id());
        assertEquals("c1", messageDeleteEventRef.get().channelId());

        assertEquals("g1", guildCreateEventRef.get().id());
        assertEquals(42, guildCreateEventRef.get().memberCount());
        assertFalse(guildCreateEventRef.get().unavailable());

        assertEquals("i1", interactionCreateEventRef.get().id());
        assertEquals("ping", interactionCreateEventRef.get().data().name());
        assertEquals("neo", interactionCreateEventRef.get().member().user().username());
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

        CapturingWebSocket socket = new CapturingWebSocket();
        client.onText(socket, "{\"op\":10,\"d\":{\"heartbeat_interval\":45000}}", true);

        JsonNode sentPayload = mapper.readTree(socket.lastSentText.get());
        assertEquals(2, sentPayload.path("op").asInt());
        assertEquals(1, sentPayload.path("d").path("shard").get(0).asInt());
        assertEquals(4, sentPayload.path("d").path("shard").get(1).asInt());
    }

    private static DiscordGatewayClient gatewayClient(ObjectMapper mapper) {
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

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            lastSentText.set(data.toString());
            return CompletableFuture.completedFuture(this);
        }
    }
}
