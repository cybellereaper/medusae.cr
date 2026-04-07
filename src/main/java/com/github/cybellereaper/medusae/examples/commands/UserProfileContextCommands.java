package com.github.cybellereaper.medusae.examples.commands;

import com.github.cybellereaper.medusae.client.ResolvedUser;
import com.github.cybellereaper.medusae.commands.core.annotation.Command;
import com.github.cybellereaper.medusae.commands.core.annotation.Description;
import com.github.cybellereaper.medusae.commands.core.annotation.Execute;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.response.ImmediateResponse;

@Command(value = "Inspect User", type = CommandType.USER_CONTEXT)
@Description("Inspect the selected user")
public final class UserProfileContextCommands {

    @Execute
    public ImmediateResponse inspect(ResolvedUser target) {
        String username = target == null ? "unknown" : target.username();
        return ImmediateResponse.ephemeralMessage("Inspecting user: " + username);
    }
}
