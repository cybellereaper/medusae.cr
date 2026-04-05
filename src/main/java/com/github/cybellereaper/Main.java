import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.client.SlashCommandDefinition;
import com.github.cybellereaper.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.gateway.GatewayIntent;

import java.util.List;

void main() throws Exception {
    String token = System.getenv("DISCORD_BOT_TOKEN");
    String guildId = System.getenv("DISCORD_GUILD_ID");

    DiscordClientConfig config = DiscordClientConfig.builder(token)
            .intents(GatewayIntent.combine(
                    GatewayIntent.GUILDS,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGES
            ))
            .build();

    try (DiscordClient client = DiscordClient.create(config)) {
        List<SlashCommandDefinition> commands = List.of(
                SlashCommandDefinition.simple("ping", "Reply with pong"),
                new SlashCommandDefinition(
                        "echo",
                        "Echo input text back",
                        List.of(SlashCommandOptionDefinition.string("text", "Text to echo", true))
                )
        );

        if (guildId == null || guildId.isBlank()) {
            client.registerGlobalSlashCommands(commands);
        } else {
            client.registerGuildSlashCommands(guildId, commands);
        }

        client.on("MESSAGE_CREATE", message -> {
            String content = message.path("content").asText("");
            String channelId = message.path("channel_id").asText();

            if ("!ping".equals(content)) {
                client.sendMessage(channelId, "pong");
            }
        });

        client.onSlashCommand("ping", interaction -> client.respondWithMessage(interaction, "pong"));
        client.onSlashCommand("echo", interaction -> {
            String text = client.getStringOption(interaction, "text");
            if (text == null || text.isBlank()) {
                client.respondEphemeral(interaction, "Missing required option: text");
                return;
            }

            client.respondWithMessage(interaction, text);
        });

        client.onComponentInteraction("confirm_button", interaction -> client.respondEphemeral(interaction, "Confirmed ✅"));
        client.onModalSubmit("feedback_modal", interaction ->
                client.respondEphemeral(interaction, "Thanks for the feedback!"));

        client.login();
        Thread.currentThread().join();
    }
}
