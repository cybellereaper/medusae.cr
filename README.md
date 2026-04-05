# Jellycord

Jellycord is a lightweight Java client for building Discord bots using both the Gateway and REST APIs.

## Features

- Gateway connection and event subscription
- Slash commands, context menus, autocomplete, component interactions, and modals
- Message builders for embeds and components
- REST helpers for common Discord resources through `DiscordApi`

## Installation

Add Jellycord as a dependency in your Gradle build (replace with your published version once released):

```gradle
implementation 'com.github.cybellereaper:jellycord:1.0.0'
```

## Quick Start

```java
String token = System.getenv("DISCORD_BOT_TOKEN");

DiscordClientConfig config = DiscordClientConfig.builder(token)
        .intents(GatewayIntent.combine(GatewayIntent.GUILDS, GatewayIntent.GUILD_MESSAGES))
        .build();

try (DiscordClient client = DiscordClient.create(config)) {
    client.onSlashCommand("ping", interaction -> client.respondWithMessage(interaction, "pong"));

    client.registerGlobalSlashCommand("ping", "Reply with pong");
    client.login();
    Thread.currentThread().join();
}
```

## Sharding

Configure shard routing when running multiple gateway workers:

```java
DiscordClientConfig config = DiscordClientConfig.builder(token)
        .intents(GatewayIntent.combine(GatewayIntent.GUILDS, GatewayIntent.GUILD_MESSAGES))
        .shard(1, 4) // shardId=1 out of 4 total shards
        .build();
```

When `shardCount > 1`, Jellycord includes the Discord gateway `shard` tuple during identify.

## OAuth Scopes (Java 25 API Surface)

Use `DiscordOAuthScopes` to build deterministic scope strings:

```java
String scopes = DiscordOAuthScopes.join(
        DiscordOAuthScopes.BOT,
        DiscordOAuthScopes.APPLICATIONS_COMMANDS
);
```


## Typed Gateway Events

Jellycord supports both raw `JsonNode` listeners and typed models for common events:

```java
import com.github.cybellereaper.gateway.events.GuildCreateEvent;
import com.github.cybellereaper.gateway.events.InteractionCreateEvent;
import com.github.cybellereaper.gateway.events.MessageCreateEvent;
import com.github.cybellereaper.gateway.events.MessageDeleteEvent;
import com.github.cybellereaper.gateway.events.ReadyEvent;

client.on("READY", ReadyEvent.class, ready ->
        System.out.println("Session: " + ready.sessionId()));

client.on("MESSAGE_CREATE", MessageCreateEvent.class, event ->
        System.out.println(event.author().username() + ": " + event.content()));

client.on("MESSAGE_DELETE", MessageDeleteEvent.class, event ->
        System.out.println("Deleted message " + event.id() + " in channel " + event.channelId()));

client.on("GUILD_CREATE", GuildCreateEvent.class, event ->
        System.out.println("Connected to guild " + event.name() + " with " + event.memberCount() + " members"));

client.on("INTERACTION_CREATE", InteractionCreateEvent.class, event -> {
    if (event.data() != null) {
        System.out.println("Interaction: " + event.data().name());
    }
});
```

For uncommon payloads, keep using raw listeners:

```java
client.on("THREAD_CREATE", payload -> System.out.println(payload));
```

## Modal Support

You can open a modal from any interaction and read submitted values on modal submit events:

```java
client.onSlashCommand("feedback", interaction -> {
    DiscordModal modal = DiscordModal.of(
            "feedback_modal",
            "Feedback",
            List.of(DiscordActionRow.of(List.of(
                    DiscordTextInput.paragraph("feedback_text", "What can we improve?")
                            .withLengthRange(10, 1000)
            )))
    );

    client.respondWithModal(interaction, modal);
});

client.onModalSubmit("feedback_modal", interaction -> {
    String feedback = client.getModalValue(interaction, "feedback_text");
    client.respondEphemeral(interaction, "Thanks for your feedback: " + feedback);
});
```

## REST API Helper

Use `client.api()` for convenient access to common REST resources:

```java
JsonNode currentUser = client.api().getCurrentUser();
JsonNode channel = client.api().getChannel("1234567890");
client.api().deleteMessage("1234567890", "9876543210");
```

For custom calls, use:

```java
JsonNode response = client.api().request("GET", "/guilds/1234567890", null);
```

## Running tests

```bash
./gradlew test
```
