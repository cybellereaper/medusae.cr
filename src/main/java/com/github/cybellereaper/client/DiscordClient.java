package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.gateway.DiscordGatewayClient;
import com.github.cybellereaper.http.DiscordRestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class DiscordClient implements AutoCloseable {
    private final DiscordRestClient restClient;
    private final DiscordGatewayClient gatewayClient;
    private final SlashCommandRouter slashCommandRouter;

    private volatile String applicationId;

    private DiscordClient(DiscordRestClient restClient, DiscordGatewayClient gatewayClient) {
        this.restClient = restClient;
        this.gatewayClient = gatewayClient;
        this.slashCommandRouter = new SlashCommandRouter(restClient::createInteractionResponse);
        this.gatewayClient.on("INTERACTION_CREATE", slashCommandRouter::handleInteraction);
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

    public void onSlashCommand(String commandName, Consumer<JsonNode> listener) {
        slashCommandRouter.registerSlashHandler(commandName, listener);
    }

    public void onComponentInteraction(String customId, Consumer<JsonNode> listener) {
        slashCommandRouter.registerComponentHandler(customId, listener);
    }

    public void onModalSubmit(String customId, Consumer<JsonNode> listener) {
        slashCommandRouter.registerModalHandler(customId, listener);
    }

    public JsonNode registerGlobalSlashCommand(String commandName, String description) {
        return registerGlobalSlashCommand(SlashCommandDefinition.simple(commandName, description));
    }

    public JsonNode registerGlobalSlashCommand(SlashCommandDefinition command) {
        Objects.requireNonNull(command, "command");
        return restClient.createGlobalApplicationCommand(resolveApplicationId(), command);
    }

    public void registerGlobalSlashCommands(List<SlashCommandDefinition> commands) {
        registerCommands(commands, this::registerGlobalSlashCommand);
    }

    public JsonNode registerGuildSlashCommand(String guildId, String commandName, String description) {
        return registerGuildSlashCommand(guildId, SlashCommandDefinition.simple(commandName, description));
    }

    public JsonNode registerGuildSlashCommand(String guildId, SlashCommandDefinition command) {
        requireNonBlank(guildId, "guildId");
        Objects.requireNonNull(command, "command");

        return restClient.createGuildApplicationCommand(resolveApplicationId(), guildId, command);
    }

    public void registerGuildSlashCommands(String guildId, List<SlashCommandDefinition> commands) {
        requireNonBlank(guildId, "guildId");
        registerCommands(commands, command -> registerGuildSlashCommand(guildId, command));
    }

    public void respondWithMessage(JsonNode interaction, String content) {
        slashCommandRouter.respondWithMessage(interaction, content);
    }

    public void respondEphemeral(JsonNode interaction, String content) {
        slashCommandRouter.respondEphemeral(interaction, content);
    }

    public void deferMessage(JsonNode interaction) {
        slashCommandRouter.deferMessage(interaction);
    }

    public void deferUpdate(JsonNode interaction) {
        slashCommandRouter.deferUpdate(interaction);
    }

    public String getStringOption(JsonNode interaction, String optionName) {
        return slashCommandRouter.getOptionString(interaction, optionName);
    }

    public void sendMessage(String channelId, String content) {
        restClient.sendMessage(channelId, content);
    }

    @Override
    public void close() {
        gatewayClient.close();
    }

    private void registerCommands(List<SlashCommandDefinition> commands, Consumer<SlashCommandDefinition> registrar) {
        Objects.requireNonNull(commands, "commands");
        for (SlashCommandDefinition command : commands) {
            registrar.accept(command);
        }
    }

    private String resolveApplicationId() {
        String current = applicationId;
        if (current != null && !current.isBlank()) {
            return current;
        }

        synchronized (this) {
            if (applicationId == null || applicationId.isBlank()) {
                applicationId = restClient.getCurrentApplicationId();
            }
            return applicationId;
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
