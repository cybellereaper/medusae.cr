import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.discord.adapter.DiscordCommandDispatcher;
import com.github.cybellereaper.commands.discord.adapter.DiscordFrameworkBinder;
import com.github.cybellereaper.commands.discord.sync.DiscordCommandSyncService;
import com.github.cybellereaper.examples.commands.ExampleCommandBootstrap;
import com.github.cybellereaper.gateway.GatewayIntent;

void main() throws Exception {
    String token = System.getenv("DISCORD_BOT_TOKEN");
    String guildId = System.getenv("DISCORD_GUILD_ID");

    if (token == null || token.isBlank()) {
        throw new IllegalStateException("DISCORD_BOT_TOKEN must be set");
    }

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

        DiscordFrameworkBinder.bind(client, framework, dispatcher);

        client.login();
        Thread.currentThread().join();
    }
}
