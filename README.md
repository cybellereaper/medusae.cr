# Jellycord

Jellycord is a Java library for building Discord bots with **two complementary command frameworks**:

1. **Interaction Router API** (`DiscordClient` + `InteractionContext`) for lightweight, handler-first bots.
2. **Annotation Command Framework** (`com.github.cybellereaper.commands`) for typed, modular command systems with checks, cooldowns, and schema sync.

Both frameworks run on the same gateway + REST core, so you can start simple and migrate to annotations when your command surface grows.

## What you get

- Gateway lifecycle + event subscription
- Slash commands, context menus, autocomplete, components, and modals
- High-level interaction response helpers
- REST convenience API (`DiscordApi`) for common resources
- Retry/backoff + rate-limit observability hooks
- Optional in-memory state cache
- Attachment upload helpers
- Voice transport primitives for gateway/audio frame workflows

## Installation

```gradle
implementation 'com.github.cybellereaper:jellycord:1.0.0'
```

---

## Framework selection guide

Use this to choose the right abstraction for your bot:

| If you want... | Use... |
|---|---|
| Small bot, direct handlers, minimum abstraction | **Interaction Router API** |
| Declarative commands, reusable checks/resolvers, cleaner scaling | **Annotation Command Framework** |
| Full control over Discord endpoints | `DiscordApi` (with either framework) |

---

## Quick start: Interaction Router API

```java
String token = System.getenv("DISCORD_BOT_TOKEN");

DiscordClientConfig config = DiscordClientConfig.builder(token)
        .intents(GatewayIntent.combine(GatewayIntent.GUILDS, GatewayIntent.GUILD_MESSAGES))
        .build();

try (DiscordClient client = DiscordClient.create(config)) {
    client.onSlashCommandContext("ping", ctx -> ctx.respondWithMessage("pong"));

    client.onSlashCommandContext("echo", ctx -> {
        String text = ctx.requiredOptionString("text");
        ctx.respondEphemeral("You said: " + text);
    });

    client.registerGlobalSlashCommand("ping", "Reply with pong");
    client.registerGlobalSlashCommand("echo", "Echo text");

    client.login();
    Thread.currentThread().join();
}
```

### Interaction Router highlights

- Register handlers with `on*Context(...)`
- Use `InteractionContext` to read options and send replies
- Supports slash commands, component interactions, autocomplete, and modal submit handlers

---

## Quick start: Annotation Command Framework

```java
CommandFramework framework = new CommandFramework();

framework.registerCheck("guildonly", ctx -> !ctx.interaction().dm());
framework.registerAutocomplete("membersearch", (ctx, value) -> List.of("alice", "bob"));
framework.registerCommands(new UserCommands());

DiscordCommandSyncService sync = new DiscordCommandSyncService(framework);
sync.syncGlobal(discordClient);
```

### Annotation framework highlights

- Slash + subcommands + subcommand groups
- User/message context commands
- Typed parameter binding with custom resolvers
- Declarative checks, permissions, cooldowns, and autocomplete
- Discord schema exporter + sync service

Read more in [`docs-command-framework.md`](docs-command-framework.md).

---


## Annotation Gateway Events

You can also register gateway listeners through annotations, using the same module-centric style as command annotations.

```java
AnnotatedGatewayEventBinder binder = new AnnotatedGatewayEventBinder();
binder.bind(discordClient, new ModerationEvents());
```

```java
@EventModule
public final class ModerationEvents {
    @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
    public void onReady(ReadyEvent event) {
        System.out.println("Session: " + event.sessionId());
    }

    @OnGatewayEvent(value = "MESSAGE_CREATE", payload = MessageCreateEvent.class)
    public void onMessage(MessageCreateEvent event, DiscordClient client) {
        if ("!ping".equals(event.content())) {
            client.api().sendMessage(event.channelId(), DiscordMessage.ofContent("pong"));
        }
    }
}
```

Handler signature rules:
- Exactly one payload parameter compatible with `payload()`
- Optional `DiscordClient` parameter for client access
- Handler methods cannot be private

## Core configuration examples

### Sharding

```java
DiscordClientConfig config = DiscordClientConfig.builder(token)
        .intents(GatewayIntent.combine(GatewayIntent.GUILDS, GatewayIntent.GUILD_MESSAGES))
        .shard(1, 4) // shardId=1 out of 4 total shards
        .build();
```

### OAuth scopes

```java
String scopes = DiscordOAuthScopes.join(
        DiscordOAuthScopes.BOT,
        DiscordOAuthScopes.APPLICATIONS_COMMANDS
);
```

### Reliability hooks

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

### REST convenience access

```java
JsonNode currentUser = client.api().getCurrentUser();
JsonNode channel = client.api().getChannel("1234567890");
client.api().deleteMessage("1234567890", "9876543210");
```

### Attachment uploads

```java
client.sendMessageWithAttachments(
        "123",
        DiscordMessage.ofContent("upload"),
        List.of(DiscordAttachment.fromPath(Path.of("/tmp/demo.png")))
);
```

---

## Additional docs

- API reference: [`API.md`](API.md)
- Annotation command framework details: [`docs-command-framework.md`](docs-command-framework.md)
- Examples: `src/main/java/com/github/cybellereaper/examples/commands`

## Running tests

```bash
./gradlew test
```
