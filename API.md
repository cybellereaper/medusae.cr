# Jellycord API Reference (Core)

## `DiscordClient`

`DiscordClient` is the main entry point for creating a bot client.

### Core lifecycle

- `DiscordClient.create(DiscordClientConfig config)`
- `login()`
- `close()`

### Config highlights (`DiscordClientConfig.Builder`)

- `intents(int intents)`
- `shard(int shardId, int shardCount)` for gateway sharding.

### Event handling

- `on(String eventType, Consumer<JsonNode> listener)`
- `on(String eventType, Class<T> eventClass, Consumer<T> listener)`
- `off(...)`

### Command registration

- `registerGlobalSlashCommand(...)`
- `registerGuildSlashCommand(...)`
- `registerGlobalSlashCommands(List<SlashCommandDefinition>)`
- `registerGuildSlashCommands(String guildId, List<SlashCommandDefinition>)`

### Interaction responses

- Prefer `on*Context(...)` handlers and respond through `InteractionContext`.
- `InteractionContext` helpers include:
  - Response helpers: `respondWithMessage`, `respondWithEmbeds`, `respondEphemeral`, `respondWithModal`, `respondWithAutocompleteChoices`, `deferMessage`, `deferUpdate`
  - Option helpers: `optionString`, `requiredOptionString`, `optionLong`, `optionInt`, `optionBoolean`, `optionDouble`
  - Resolved entity helpers: `resolvedAttachment`, `resolvedUser`, `resolvedMember`, `resolvedRole`, `resolvedChannel`
  - Typed resolved wrappers: `resolvedAttachmentValue`, `resolvedUserValue`, `resolvedMemberValue`, `resolvedRoleValue`, `resolvedChannelValue`
  - Option-to-resolved helpers: `optionResolvedAttachment`, `optionResolvedUser`, `optionResolvedRole`, `optionResolvedChannel`
  - Typed option-to-resolved wrappers: `optionResolvedAttachmentValue`, `optionResolvedUserValue`, `optionResolvedRoleValue`, `optionResolvedChannelValue`
  - Modal helper: `modalValue`
  - Metadata helpers: `id`, `token`, `interactionType`, `commandType`, `guildId`, `channelId`, `userId`

- Legacy raw `JsonNode` interaction helpers are still available but deprecated.

### Message sending

- `sendMessage(...)`
- `sendMessageWithEmbeds(...)`

### REST helper access

- `api()` returns a `DiscordApi` instance for common direct REST operations.


## Interaction Routing Examples

### Slash command routing with `InteractionContext`

```java
client.onSlashCommandContext("echo", context -> {
    String text = context.requiredOptionString("text");
    context.respondEphemeral("Echo: " + text);
});
```

### Context menu routing

```java
client.onUserContextMenuContext("Inspect User", context ->
        context.respondEphemeral("Triggered by user " + context.userId())
);

client.onMessageContextMenuContext("Quote Message", context ->
        context.respondWithMessage("Quoted from context menu")
);
```

### Component/modal/autocomplete handling

```java
client.onComponentInteractionContext("open_modal", context -> context.respondWithModal(modal));
client.onModalSubmitContext("feedback_modal", context -> context.respondEphemeral(context.modalValue("feedback")));
client.onAutocompleteContext("search", context -> context.respondWithAutocompleteChoices(List.of()));
```

## `DiscordApi`

`DiscordApi` provides convenience methods on top of the underlying REST client.

- `getCurrentApplication()`
- `getCurrentUser()`
- `getChannel(String channelId)`
- `getGuild(String guildId)`
- `deleteMessage(String channelId, String messageId)`
- `request(String method, String path, Map<String, Object> body)`

### Validation rules

- IDs and required string arguments must be non-blank.
- `request(..., path, ...)` requires `path` to start with `/`.

## `DiscordOAuthScopes`

Helpers for OAuth scope composition:

- `join(String... scopes)` for normalized, deduplicated scopes.
- `defaultBotScopes()` for `bot` + `applications.commands`.
