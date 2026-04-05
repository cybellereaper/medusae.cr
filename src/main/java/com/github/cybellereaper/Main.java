import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.gateway.GatewayIntent;

void main() throws Exception {
    String token = System.getenv("DISCORD_BOT_TOKEN");

    DiscordClientConfig config = DiscordClientConfig.builder(token)
            .intents(GatewayIntent.combine(GatewayIntent.GUILDS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES))
            .build();

    try (DiscordClient client = DiscordClient.create(config)) {
        client.on("MESSAGE_CREATE", message -> {
            String content = message.path("content").asText("");
            String channelId = message.path("channel_id").asText();

            if ("!ping".equals(content)) {
                client.sendMessage(channelId, "pong");
            }
        });

        client.login();
        Thread.currentThread().join();
    }
}
