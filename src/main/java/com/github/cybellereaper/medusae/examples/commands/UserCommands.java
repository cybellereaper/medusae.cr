package com.github.cybellereaper.medusae.examples.commands;

import com.github.cybellereaper.medusae.client.ResolvedMember;
import com.github.cybellereaper.medusae.client.ResolvedUser;
import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.execute.CommandContext;
import com.github.cybellereaper.medusae.commands.core.response.ImmediateResponse;

import java.util.List;

@Command("user")
@Description("User management commands")
@GuildOnly
public final class UserCommands {

    @Execute
    @Description("Shows framework status")
    public ImmediateResponse root() {
        return ImmediateResponse.ephemeralMessage("Use /user ban, /user info, or /user note");
    }

    @Subcommand("ban")
    @Description("Ban a member from the server")
    @RequireUserPermissions("ban_members")
    public void ban(
            CommandContext ctx,
            @Name("target") @Description("Member to ban") ResolvedMember target,
            @Name("reason") @Optional @Autocomplete("common-reasons") String reason
    ) {
        String effectiveReason = reason == null ? "No reason provided" : reason;
        String targetId = target == null ? "unknown" : target.userId();
        ctx.reply("Banned <@" + targetId + "> for: " + effectiveReason);
    }

    @Subcommand("info")
    @Description("Show information about a user")
    public void info(CommandContext ctx, @Name("target") @Description("User to inspect") ResolvedUser target) {
        String username = target == null ? "unknown" : target.username();
        String userId = target == null ? "unknown" : target.id();
        ctx.replyEphemeral("User: " + username + " (" + userId + ")");
    }

    @SubcommandGroup("moderation")
    @Subcommand("note")
    @Description("Store a moderation note")
    @Check("in-guild")
    public void note(
            CommandContext ctx,
            @Name("target") ResolvedUser target,
            @Name("note") @Default("No note provided") String note
    ) {
        String targetName = target == null ? "unknown" : target.username();
        ctx.reply("Saved note for " + targetName + ": " + note);
    }

    @Autocomplete("reason")
    public List<String> reasonSuggestions(CommandContext ctx) {
        return List.of("Spam", "Harassment", "Raid", "Phishing");
    }
}
