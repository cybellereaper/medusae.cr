# Sealed-model modernization audit (core model + interaction responses)

This note captures candidates reviewed in `commands/core/model` and interaction result/response modeling.

## Accepted candidates

1. **`InteractionContext` hierarchy** (`ComponentContext`, `ModalContext`, `SelectContext`)
   - **Why accepted**: The subtype set is closed and internal to the command framework.
   - **Change**: `InteractionContext` is now a sealed abstract class with an exhaustive `switch` factory (`InteractionContext.from(...)`) keyed by `InteractionHandlerType`.
   - **Benefit**: Compile-time coverage when new interaction handler types are added; context construction logic is centralized.

## Rejected candidates (for now)

1. **Discord payload/option mappings** (e.g., Discord adapter payload types and option type codes)
   - **Why rejected**: These are interop-sensitive and effectively coupled to Discord wire contracts.
   - **Guardrail**: Do not convert these to sealed hierarchies until contract/integration test coverage is broadened.

2. **`CommandOptionValue` (`Object value`, `int discordType`)**
   - **Why rejected**: A typed sealed value model would be cleaner, but this object currently bridges multiple upstream payload shapes and command-option coercion paths.
   - **Risk**: Tightening this too early risks regressions in mapper behavior without additional Discord payload contract tests.

3. **`CommandResponder` interface**
   - **Why rejected**: Not a closed-domain model; intentionally open for external adapters and test doubles.

## Follow-up opportunities

- If contract tests are expanded around Discord payload translation and option coercion, revisit `CommandOptionValue` to introduce a sealed internal option value hierarchy.
- Keep `InteractionContext.from(...)` as the single construction point so enum growth fails fast at compile time.
