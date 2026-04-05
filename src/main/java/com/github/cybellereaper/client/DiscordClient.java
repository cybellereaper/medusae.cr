package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.gateway.DiscordGatewayClient;
import com.github.cybellereaper.http.DiscordRestClient;

import java.net.http.HttpClient;
import java.util.function.Consumer;

public final class DiscordClient implements AutoCloseable {
    private final DiscordRestClient restClient;
    private final DiscordGatewayClient gatewayClient;

    private DiscordClient(DiscordRestClient restClient, DiscordGatewayClient gatewayClient) {
        this.restClient = restClient;
        this.gatewayClient = gatewayClient;
    }

    public static DiscordClient create(DiscordClientConfig config) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        ObjectMapper objectMapper = new ObjectMapper();

        DiscordRestClient restClient = new DiscordRestClient(httpClient, objectMapper, config);
        DiscordGatewayClient gatewayClient = new DiscordGatewayClient(httpClient, objectMapper, config, restClient);

        return new DiscordClient(restClient, gatewayClient);
    }

    public void login() {
        gatewayClient.connect();
    }

    public void on(String eventType, Consumer<JsonNode> listener) {
        gatewayClient.on(eventType, listener);
    }

    public void sendMessage(String channelId, String content) {
        restClient.sendMessage(channelId, content);
    }

    @Override
    public void close() {
        gatewayClient.close();
    }
}
