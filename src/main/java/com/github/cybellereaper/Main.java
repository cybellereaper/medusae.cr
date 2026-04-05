import com.github.cybellereaper.client.AutocompleteChoice;
import com.github.cybellereaper.client.DiscordActionRow;
import com.github.cybellereaper.client.DiscordButton;
import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordClientConfig;
import com.github.cybellereaper.client.DiscordEmbed;
import com.github.cybellereaper.client.DiscordMessage;
import com.github.cybellereaper.client.DiscordPermissions;
import com.github.cybellereaper.client.DiscordSelectOption;
import com.github.cybellereaper.client.DiscordStringSelectMenu;
import com.github.cybellereaper.client.SlashCommandDefinition;
import com.github.cybellereaper.client.SlashCommandOptionDefinition;
import com.github.cybellereaper.gateway.GatewayIntent;
import com.github.cybellereaper.gateway.events.GuildCreateEvent;
import com.github.cybellereaper.gateway.events.InteractionCreateEvent;
import com.github.cybellereaper.gateway.events.MessageCreateEvent;
import com.github.cybellereaper.gateway.events.MessageDeleteEvent;
import com.github.cybellereaper.gateway.events.ReadyEvent;

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
                SlashCommandDefinition.simple("ping", "Reply with pong")
                        .withDefaultMemberPermissions(DiscordPermissions.of(
                                DiscordPermissions.SEND_MESSAGES,
                                DiscordPermissions.USE_APPLICATION_COMMANDS
                        )),
                new SlashCommandDefinition(
                        "echo",
                        "Echo input text back",
                        List.of(SlashCommandOptionDefinition.autocompletedString("text", "Text to echo", true))
                ),
                SlashCommandDefinition.userContextMenu("Inspect User"),
                SlashCommandDefinition.messageContextMenu("Quote Message")
        );

        if (guildId == null || guildId.isBlank()) {
            client.registerGlobalSlashCommands(commands);
        } else {
            client.registerGuildSlashCommands(guildId, commands);
        }

        client.on("READY", ReadyEvent.class, ready ->
                System.out.println("Gateway ready, session " + ready.sessionId()));

        client.on("GUILD_CREATE", GuildCreateEvent.class, guild ->
                System.out.println("Connected to guild " + guild.name() + " (" + guild.id() + ")"));

        client.on("INTERACTION_CREATE", InteractionCreateEvent.class, interaction -> {
            if (interaction.data() != null) {
                System.out.println("Incoming interaction: " + interaction.data().name());
            }
        });

        client.on("MESSAGE_DELETE", MessageDeleteEvent.class, event ->
                System.out.println("Deleted message " + event.id()));

        client.on("MESSAGE_CREATE", MessageCreateEvent.class, message -> {
            String content = message.content() == null ? "" : message.content();
            String channelId = message.channelId();

            if ("!ping".equals(content) && channelId != null) {
                DiscordActionRow buttons = DiscordActionRow.of(List.of(
                        DiscordButton.primary("confirm_button", "Confirm").withEmoji("✅"),
                        DiscordButton.link("https://discord.com/developers/docs/interactions", "Docs")
                ));

                client.sendMessage(channelId, DiscordMessage.ofEmbeds("pong", List.of(
                                new DiscordEmbed("Legacy Ping", "Handled via message command", 0x57F287)
                                        .withImage("https://images.unsplash.com/photo-1516117172878-fd2c41f4a759")
                        ))
                        .withComponents(List.of(buttons)));
            }
        });

        client.onSlashCommand("ping", interaction -> {
            DiscordActionRow selectMenuRow = DiscordActionRow.of(List.of(
                    DiscordStringSelectMenu.of("theme_select", List.of(
                                    DiscordSelectOption.of("Light", "light"),
                                    DiscordSelectOption.of("Dark", "dark").asDefault(),
                                    DiscordSelectOption.of("System", "system")
                            ))
                            .withPlaceholder("Choose a theme")
                            .withSelectionRange(1, 1)
            ));

            client.respondWithMessage(interaction,
                    DiscordMessage.ofEmbeds("pong", List.of(
                                    new DiscordEmbed("Slash Ping", "Interaction response", 0x5865F2)
                                            .withThumbnail("https://cdn.discordapp.com/embed/avatars/0.png")
                            ))
                            .withComponents(List.of(selectMenuRow)));
        });

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

        client.onUserContextMenu("Inspect User", interaction ->
                client.respondEphemeral(interaction, "User inspection invoked."));

        client.onMessageContextMenu("Quote Message", interaction ->
                client.respondEphemeral(interaction, "Message quote command invoked."));

        client.onComponentInteraction("confirm_button", interaction ->
                client.respondEphemeral(interaction, "Confirmed!"));

        client.onComponentInteraction("theme_select", interaction ->
                client.respondEphemeral(interaction, "Theme updated."));

        client.onModalSubmit("feedback_modal", interaction ->
                client.respondEphemeralWithEmbeds(interaction, "Thanks for the feedback!", List.of(
                        new DiscordEmbed("Feedback", "Received successfully", 0x57F287)
                                .withThumbnail("https://cdn.discordapp.com/embed/avatars/1.png")
                )));

        client.login();
        Thread.currentThread().join();
    }
}
