package com.github.cybellereaper.examples.commands;

import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.core.model.CommandType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExampleCommandBootstrapTest {

    @Test
    void registersShowcaseCommandsAndTypes() {
        CommandFramework framework = ExampleCommandBootstrap.createFramework();

        assertEquals(3, framework.registry().all().size());
        assertTrue(framework.registry().find("user").isPresent());
        assertEquals(CommandType.CHAT_INPUT, framework.registry().find("user").orElseThrow().type());

        assertEquals(CommandType.USER_CONTEXT, framework.registry().find("inspect user").orElseThrow().type());
        assertEquals(CommandType.MESSAGE_CONTEXT, framework.registry().find("quote message").orElseThrow().type());
    }
}
