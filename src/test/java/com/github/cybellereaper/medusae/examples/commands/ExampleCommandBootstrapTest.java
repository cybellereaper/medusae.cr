package com.github.cybellereaper.medusae.examples.commands;

import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExampleCommandBootstrapTest {

    @Test
    void registersShowcaseCommandsAndTypes() {
        CommandFramework framework = ExampleCommandBootstrap.createFramework();

        assertEquals(4, framework.registry().all().size());
        assertTrue(framework.registry().find("user").isPresent());
        assertEquals(CommandType.CHAT_INPUT, framework.registry().find("user").orElseThrow().type());
        assertEquals(CommandType.CHAT_INPUT, framework.registry().find("ticket").orElseThrow().type());

        assertEquals(CommandType.USER_CONTEXT, framework.registry().find("inspect user").orElseThrow().type());
        assertEquals(CommandType.MESSAGE_CONTEXT, framework.registry().find("quote message").orElseThrow().type());
    }

    @Test
    void registersTicketComponentAndModalInteractionHandlers() {
        CommandFramework framework = ExampleCommandBootstrap.createFramework();

        assertTrue(framework.interactionRegistry().find(InteractionHandlerType.BUTTON, "ticket:create").isPresent());
        assertTrue(framework.interactionRegistry().find(InteractionHandlerType.MODAL, "ticket:create").isPresent());
        assertTrue(framework.interactionRegistry().find(InteractionHandlerType.BUTTON, "ticket:close:42").isPresent());
    }
}
