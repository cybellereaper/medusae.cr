package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.commands.core.annotation.*;
import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ComponentContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ModalContext;
import com.github.cybellereaper.medusae.commands.core.model.InteractionExecution;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import com.github.cybellereaper.medusae.commands.core.model.ResolvedEntities;
import com.github.cybellereaper.medusae.commands.core.response.CommandResponse;
import com.github.cybellereaper.medusae.commands.core.response.ImmediateResponse;
import com.github.cybellereaper.medusae.commands.core.response.InteractionReply;
import com.github.cybellereaper.medusae.commands.core.response.ModalReply;
import com.github.cybellereaper.medusae.commands.core.response.component.ActionRowSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.ButtonSpec;
import com.github.cybellereaper.medusae.commands.core.response.embed.EmbedSpec;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InteractionExecutionFlowTest {

    @Test
    void executesCommandButtonModalFlow() {
        CommandFramework framework = new CommandFramework();
        framework.registerModules(new TicketModule());

        AtomicReference<CommandResponse> response = new AtomicReference<>();
        framework.executeInteraction(new InteractionExecution(
                InteractionHandlerType.BUTTON,
                "ticket:create",
                Map.of(),
                null,
                ResolvedEntities.empty(),
                true,
                null,
                "user-1",
                Set.of(),
                Set.of(),
                null
        ), response::set);

        assertInstanceOf(ModalReply.class, response.get());

        framework.executeInteraction(new InteractionExecution(
                InteractionHandlerType.MODAL,
                "ticket:create",
                Map.of("subject", "Need help", "details", "Details"),
                null,
                ResolvedEntities.empty(),
                true,
                null,
                "user-1",
                Set.of(),
                Set.of(),
                null
        ), response::set);

        assertInstanceOf(InteractionReply.class, response.get());
        InteractionReply reply = (InteractionReply) response.get();
        assertTrue(reply.isEphemeral());
        assertEquals("Ticket created: Need help", reply.content());
    }

    @Test
    void mapsStringReturnValueToImmediateResponse() {
        CommandFramework framework = new CommandFramework();
        framework.registerModules(new PlainButtonModule());

        AtomicReference<CommandResponse> response = new AtomicReference<>();
        framework.executeInteraction(new InteractionExecution(
                InteractionHandlerType.BUTTON,
                "plain:ok",
                Map.of(),
                null,
                ResolvedEntities.empty(),
                true,
                null,
                "user-1",
                Set.of(),
                Set.of(),
                null
        ), response::set);

        assertInstanceOf(ImmediateResponse.class, response.get());
    }

    @Command("ticket")
    static final class TicketModule {
        @Execute
        InteractionReply root() {
            return InteractionReply.ephemeral()
                    .content("Open a support ticket")
                    .embed(EmbedSpec.success("Support", "Choose an action below"))
                    .components(ActionRowSpec.of(ButtonSpec.primary("ticket:create", "Create Ticket")))
                    .build();
        }

        @ButtonHandler("ticket:create")
        ModalReply openTicketModal(ComponentContext ignored) {
            return ModalReply.create("ticket:create")
                    .title("Create Ticket")
                    .textInput("subject", "Subject", true)
                    .paragraphInput("details", "Details", true)
                    .build();
        }

        @ModalHandler("ticket:create")
        InteractionReply submitTicket(ModalContext ctx, @Field("subject") String subject, @Field("details") String details) {
            return InteractionReply.ephemeral().content("Ticket created: " + subject).build();
        }
    }

    static final class PlainButtonModule {
        @ButtonHandler("plain:ok")
        String press() {
            return "ok";
        }
    }
}
