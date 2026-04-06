# Command Recipes

This page collects practical patterns for interaction command handling.

## 1) Permission-aware slash command

```java
client.onSlashCommandContext("admin-ban", context -> {
    String invokerId = context.userId();
    if (!adminIds.contains(invokerId)) {
        context.respondEphemeral("You do not have permission to run this command.");
        return;
    }

    String userId = context.requiredOptionString("user_id");
    String reason = context.optionString("reason");
    context.respondWithMessage("Ban queued for " + userId + (reason == null ? "" : " (" + reason + ")"));
});
```

## 2) Deferred reply for longer work

```java
client.onSlashCommandContext("report", context -> {
    context.deferMessage();

    // Do slower domain work asynchronously in your own executor.
    reportService.buildReportAsync(context.userId())
            .thenAccept(report -> context.respondWithMessage("Report ready: " + report.summary()))
            .exceptionally(ex -> {
                context.respondEphemeral("Could not build report right now.");
                return null;
            });
});
```

## 3) Error mapping at the handler boundary

```java
client.onSlashCommandContext("balance", context -> {
    try {
        long userBalance = walletService.balanceFor(context.userId());
        context.respondEphemeral("Balance: " + userBalance);
    } catch (UnknownAccountException ex) {
        context.respondEphemeral("No wallet exists for this account yet.");
    } catch (Exception ex) {
        context.respondEphemeral("Unexpected error. Please try again later.");
    }
});
```

## 4) Autocomplete with graceful fallback

```java
client.onAutocompleteContext("project", context -> {
    String prefix = context.optionString("query");

    List<AutocompleteChoice> choices = projectLookup.search(prefix).stream()
            .limit(25)
            .map(name -> new AutocompleteChoice(name, name))
            .toList();

    context.respondWithAutocompleteChoices(choices);
});
```

If an autocomplete provider throws during dispatch, Jellycord falls back to an empty choice list.
