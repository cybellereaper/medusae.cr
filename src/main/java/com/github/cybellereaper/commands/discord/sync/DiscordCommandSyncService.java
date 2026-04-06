package com.github.cybellereaper.commands.discord.sync;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.discord.schema.DiscordCommandSchemaExporter;

public final class DiscordCommandSyncService {
    private final CommandFramework framework;
    private final DiscordCommandSchemaExporter exporter;

    public DiscordCommandSyncService(CommandFramework framework) {
        this.framework = framework;
        this.exporter = new DiscordCommandSchemaExporter();
    }

    public void syncGlobal(DiscordClient client) {
        client.registerGlobalSlashCommands(exporter.export(framework.registry().all()));
    }

    public void syncGuild(DiscordClient client, String guildId) {
        client.registerGuildSlashCommands(guildId, exporter.export(framework.registry().all()));
    }
}
