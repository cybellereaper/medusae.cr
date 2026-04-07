package com.github.cybellereaper.medusae.examples.commands;

import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ComponentContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ModalContext;
import com.github.cybellereaper.medusae.commands.core.response.InteractionReply;
import com.github.cybellereaper.medusae.commands.core.response.ModalReply;
import com.github.cybellereaper.medusae.commands.core.response.component.ActionRowSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.ButtonSpec;
import com.github.cybellereaper.medusae.commands.core.response.embed.EmbedSpec;

@Command("ticket")
@Description("Support ticket commands")
public final class TicketInteractionCommands {

    @Execute
    public InteractionReply ticketHome() {
        return InteractionReply.ephemeral()
                .content("Open a support ticket")
                .embed(EmbedSpec.success("Support", "Choose an action below"))
                .components(ActionRowSpec.of(
                        ButtonSpec.primary("ticket:create", "Create Ticket"),
                        ButtonSpec.secondary("ticket:faq", "FAQ")
                ))
                .build();
    }

    @ButtonHandler("ticket:create")
    public ModalReply openTicketModal(ComponentContext ctx) {
        return ModalReply.create("ticket:create")
                .title("Create Ticket")
                .textInput("subject", "Subject", true)
                .paragraphInput("details", "Details", true)
                .build();
    }

    @ModalHandler("ticket:create")
    public InteractionReply submitTicket(
            ModalContext ctx,
            @Field("subject") String subject,
            @Field("details") String details
    ) {
        return InteractionReply.ephemeral()
                .content("Ticket created: " + subject)
                .build();
    }

    @ButtonHandler("ticket:close:{ticketId}")
    @RequireUserPermissions("manage_channels")
    public InteractionReply closeTicket(ComponentContext ctx, @PathParam("ticketId") long ticketId) {
        return InteractionReply.updateMessage()
                .content("Ticket #" + ticketId + " closed")
                .disableTriggeredComponent()
                .build();
    }
}
