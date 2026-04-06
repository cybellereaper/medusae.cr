import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.core.model.CommandType;
import com.github.cybellereaper.commands.discord.adapter.DiscordCommandDispatcher;
import com.github.cybellereaper.commands.discord.sync.DiscordCommandSyncService;
import com.github.cybellereaper.examples.commands.ExampleCommandBootstrap;
import com.github.cybellereaper.gateway.GatewayIntent;

void main() throws Exception {
    String token = System.getenv("DISCORD_BOT_TOKEN");
    String guildId = System.getenv("DISCORD_GUILD_ID");

    DiscordClientConfig config = DiscordClientConfig.builder(token)
            .intents(GatewayIntent.combine(GatewayIntent.GUILDS))
            .build();

    CommandFramework framework = ExampleCommandBootstrap.createFramework();
    DiscordCommandDispatcher dispatcher = new DiscordCommandDispatcher(framework);

    try (DiscordClient client = DiscordClient.create(config)) {
        DiscordCommandSyncService syncService = new DiscordCommandSyncService(framework);
        if (guildId == null || guildId.isBlank()) {
            syncService.syncGlobal(client);
        } else {
            syncService.syncGuild(client, guildId);
        }

        framework.registry().all().forEach(definition -> {
            switch (definition.type()) {
                case CHAT_INPUT -> {
                    client.onSlashCommandContext(definition.name(), context -> dispatcher.dispatch(context.raw(), context));
                    if (!definition.autocompleteHandlers().isEmpty()
                            || definition.handlers().stream().anyMatch(handler -> handler.parameters().stream().anyMatch(parameter -> parameter.autocompleteId() != null))) {
                        client.onAutocompleteContext(definition.name(), context -> dispatcher.dispatchAutocomplete(context.raw(), context));
                    }
                }
                case USER_CONTEXT -> client.onUserContextMenuContext(definition.name(), context -> dispatcher.dispatch(context.raw(), context));
                case MESSAGE_CONTEXT -> client.onMessageContextMenuContext(definition.name(), context -> dispatcher.dispatch(context.raw(), context));
            }
        });

        client.login();
        Thread.currentThread().join();
    }
}
