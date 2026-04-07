package com.github.cybellereaper.medusae.examples.commands;

import com.github.cybellereaper.medusae.client.ResolvedMessage;
import com.github.cybellereaper.medusae.commands.core.annotation.Command;
import com.github.cybellereaper.medusae.commands.core.annotation.Description;
import com.github.cybellereaper.medusae.commands.core.annotation.Execute;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.response.ImmediateResponse;

@Command(value = "Quote Message", type = CommandType.MESSAGE_CONTEXT)
@Description("Quote the selected message")
public final class MessageContextCommands {

    @Execute
    public ImmediateResponse quote(ResolvedMessage message) {
        String content = message == null || message.content() == null ? "(no content)" : message.content();
        return ImmediateResponse.ephemeralMessage("Quoted: " + content);
    }
}
