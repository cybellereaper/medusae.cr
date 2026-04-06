package com.github.cybellereaper.examples.commands;

import com.github.cybellereaper.client.ResolvedUser;
import com.github.cybellereaper.commands.core.annotation.Command;
import com.github.cybellereaper.commands.core.annotation.Description;
import com.github.cybellereaper.commands.core.annotation.Execute;
import com.github.cybellereaper.commands.core.model.CommandType;
import com.github.cybellereaper.commands.core.response.ImmediateResponse;

@Command(value = "Inspect User", type = CommandType.USER_CONTEXT)
@Description("Inspect the selected user")
public final class UserProfileContextCommands {

    @Execute
    public ImmediateResponse inspect(ResolvedUser target) {
        String username = target == null ? "unknown" : target.username();
        return ImmediateResponse.ephemeralMessage("Inspecting user: " + username);
    }
}
