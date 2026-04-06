# Jellycord

Jellycord is a lightweight Java client for building Discord bots using both the Gateway and REST APIs.

## Features

- Gateway connection and event subscription
- Slash commands, context menus, autocomplete, component interactions, and modals
- Message builders for embeds and components
- REST helpers for common Discord resources through `DiscordApi`
- Configurable retry/backoff for transient REST failures
- Rate-limit observability hooks (`RateLimitObserver`)
- Optional in-memory state cache for guild/channel/member snapshots
- Attachment upload convenience helpers
- Voice transport primitives for gateway/audio frame workflows

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
    client.onSlashCommandContext("ping", context -> context.respondWithMessage("pong"));

    client.registerGlobalSlashCommand("ping", "Reply with pong");
    client.login();
    Thread.currentThread().join();
}
```

Prefer the higher-level interaction API when possible:

```java
client.onSlashCommandContext("echo", context -> {
    String text = context.optionString("text");
    context.respondEphemeral("You said: " + text);
});

client.onSlashCommandContext("profile-photo", context -> {
    // Attachment options can be resolved directly from interaction payload metadata.
    var attachment = context.optionResolvedAttachmentValue("photo");
    context.respondEphemeral(attachment == null ? "No photo provided" : "Photo received: " + attachment.filename());
});
```


## Interaction Examples

### Slash command + typed option access

```java
client.onSlashCommandContext("admin-ban", context -> {
    String userId = context.requiredOptionString("user_id");
    String reason = context.optionString("reason");
    context.respondEphemeral("Queued ban for " + userId + (reason == null ? "" : " (" + reason + ")"));
});
```

### User and message context menus

```java
client.onUserContextMenuContext("Inspect User", context -> {
    String targetUserId = context.optionString("user");
    context.respondEphemeral("Inspecting user: " + targetUserId);
});

client.onMessageContextMenuContext("Quote Message", context ->
        context.respondWithMessage("Quoted via context menu")
);
```

### Autocomplete routing

```java
client.onAutocompleteContext("language", context -> {
    String prefix = context.optionString("query");
    List<AutocompleteChoice> choices = List.of(
            new AutocompleteChoice("Java", "java"),
            new AutocompleteChoice("JavaScript", "javascript")
    ).stream()
            .filter(choice -> prefix == null || choice.name().toLowerCase().startsWith(prefix.toLowerCase()))
            .toList();

    context.respondWithAutocompleteChoices(choices);
});
```

### Component + modal flow

```java
client.onComponentInteractionContext("open_feedback", context -> {
    DiscordModal modal = DiscordModal.of(
            "feedback_modal",
            "Feedback",
            List.of(DiscordActionRow.of(List.of(
                    DiscordTextInput.paragraph("feedback_text", "What can we improve?")
            )))
    );

    context.respondWithModal(modal);
});

client.onModalSubmitContext("feedback_modal", context -> {
    String feedback = context.modalValue("feedback_text");
    context.respondEphemeral("Thanks! Received: " + feedback);
});
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


## Advanced REST + Reliability

```java
RateLimitObserver observer = new RateLimitObserver() {
    @Override
    public void onRetryScheduled(String method, String path, int attempt, Duration backoff, String reason) {
        System.out.println("retry " + method + " " + path + " attempt=" + attempt + " cause=" + reason);
    }
};

DiscordClient client = DiscordClient.create(
        config,
        RetryPolicy.defaultPolicy(),
        observer,
        true // enable state cache
);
```

Use attachment uploads:

```java
client.sendMessageWithAttachments(
        "123",
        DiscordMessage.ofContent("upload"),
        List.of(DiscordAttachment.fromPath(Path.of("/tmp/demo.png")))
);
```

## Running tests

```bash
./gradlew test
```
