# Command Framework Design Note

## Annotation model
The framework uses class-level `@Command` plus method-level `@Subcommand`, `@SubcommandGroup`, `@Execute`, and `@Autocomplete` annotations. Cross-cutting guard annotations (`@GuildOnly`, `@DmOnly`, `@RequireUserPermissions`, `@RequireBotPermissions`, `@Check`, and `@Cooldown`) are composable at both class and method scope.

Parameter annotations (`@Name`, `@Optional`, `@Default`, and parameter-level `@Autocomplete`) drive option metadata and binding.

## Execution pipeline
1. A Discord interaction is mapped into a transport-neutral `CommandInteraction` model.
2. `CommandFramework` locates `CommandDefinition` and `CommandHandler` from `CommandRegistry`.
3. `CommandContext` is created with a `CommandResponder` sink.
4. Checks, permissions, and cooldown are evaluated.
5. Parameters are resolved through built-ins and registered custom resolvers.
6. The handler method is invoked via precompiled reflection metadata.
7. Return values implementing `CommandResponse` are translated by the responder.
8. Exceptions are routed through `CommandExceptionHandler`.

## Extension points
- `ResolverRegistry`: custom typed argument resolvers.
- `CheckRegistry`: app-level checks by ID.
- `AutocompleteRegistry`: option autocomplete providers by ID.
- `CommandExceptionHandler`: global exception hook.

## Discord-specific choices
- Slash command tree support includes root options, subcommands, and subcommand groups.
- User and message context commands map to Discord application command types.
- Autocomplete dispatch supports either annotation methods or registered provider IDs.
- Follow-up responses are represented in core response models; Discord application currently throws an explicit unsupported-operation error until webhook follow-up helpers are exposed.
