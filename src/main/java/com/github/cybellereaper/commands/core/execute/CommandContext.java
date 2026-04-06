package com.github.cybellereaper.commands.core.execute;

import com.github.cybellereaper.commands.core.model.CommandInteraction;
import com.github.cybellereaper.commands.core.response.DeferredResponse;
import com.github.cybellereaper.commands.core.response.FollowupResponse;
import com.github.cybellereaper.commands.core.response.ImmediateResponse;

import java.util.Objects;

public final class CommandContext {
    private final CommandInteraction interaction;
    private final CommandResponder responder;

    public CommandContext(CommandInteraction interaction, CommandResponder responder) {
        this.interaction = Objects.requireNonNull(interaction, "interaction");
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    public CommandInteraction interaction() {
        return interaction;
    }

    public void reply(String content) {
        responder.accept(ImmediateResponse.publicMessage(content));
    }

    public void replyEphemeral(String content) {
        responder.accept(ImmediateResponse.ephemeralMessage(content));
    }

    public void defer() {
        responder.accept(new DeferredResponse(false));
    }

    public void deferEphemeral() {
        responder.accept(new DeferredResponse(true));
    }

    public void followup(String content) {
        responder.accept(new FollowupResponse(content, false));
    }

    public void followupEphemeral(String content) {
        responder.accept(new FollowupResponse(content, true));
    }
}
