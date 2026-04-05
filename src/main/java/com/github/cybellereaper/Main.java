import com.github.cybellereaper.client.AutocompleteChoice;
import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.client.DiscordEmbed;
import com.github.cybellereaper.client.DiscordMessage;
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
                        List.of(SlashCommandOptionDefinition.autocompletedString("text", "Text to echo", true))
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
                client.sendMessage(channelId, DiscordMessage.ofEmbeds("pong", List.of(
                        new DiscordEmbed("Legacy Ping", "Handled via message command", 0x57F287)
                                .withImage("https://images.unsplash.com/photo-1516117172878-fd2c41f4a759")
                )));
            }
        });

        client.onSlashCommand("ping", interaction -> client.respondWithMessage(interaction,
                DiscordMessage.ofEmbeds("pong", List.of(
                        new DiscordEmbed("Slash Ping", "Interaction response", 0x5865F2)
                                .withThumbnail("https://cdn.discordapp.com/embed/avatars/0.png")
                ))));

        client.onSlashCommand("echo", interaction -> {
            String text = client.getStringOption(interaction, "text");
            if (text == null || text.isBlank()) {
                client.respondEphemeral(interaction, "Missing required option: text");
                return;
            }

            client.respondWithEmbeds(interaction, text, List.of(
                    new DiscordEmbed("Echo", text, 0xFEE75C)
                            .withUrl("https://discord.com/developers/docs")
            ));
        });

        client.onAutocomplete("echo", interaction -> {
            String prefix = client.getStringOption(interaction, "text");
            String safePrefix = prefix == null ? "" : prefix.toLowerCase();
            List<AutocompleteChoice> choices = List.of("hello", "hey", "hola", "bonjour").stream()
                    .filter(choice -> choice.startsWith(safePrefix))
                    .limit(25)
                    .map(choice -> new AutocompleteChoice(choice, choice))
                    .toList();

            client.respondWithAutocompleteChoices(interaction, choices);
        });

        client.onComponentInteraction("confirm_button", client::deferUpdate);
        client.onModalSubmit("feedback_modal", interaction ->
                client.respondEphemeralWithEmbeds(interaction, "Thanks for the feedback!", List.of(
                        new DiscordEmbed("Feedback", "Received successfully", 0x57F287)
                                .withThumbnail("https://cdn.discordapp.com/embed/avatars/1.png")
                )));

        client.login();
        Thread.currentThread().join();
    }
}
