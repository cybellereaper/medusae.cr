package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.commands.core.annotation.ButtonHandler;
import com.github.cybellereaper.medusae.commands.core.annotation.Field;
import com.github.cybellereaper.medusae.commands.core.annotation.ModalHandler;
import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import com.github.cybellereaper.medusae.commands.core.parser.InteractionModuleParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InteractionModuleParserTest {

    @Test
    void scansButtonAndModalHandlers() {
        InteractionModuleParser parser = new InteractionModuleParser();
        var handlers = parser.parse(new TicketModule());

        assertEquals(2, handlers.size());
        assertTrue(handlers.stream().anyMatch(h -> h.type() == InteractionHandlerType.BUTTON && h.route().equals("ticket:create")));
        assertTrue(handlers.stream().anyMatch(h -> h.type() == InteractionHandlerType.MODAL && h.route().equals("ticket:create")));
    }

    @Test
    void rejectsFieldParamOnNonModalHandler() {
        InteractionModuleParser parser = new InteractionModuleParser();
        assertThrows(RegistrationException.class, () -> parser.parse(new BrokenModule()));
    }

    static final class TicketModule {
        @ButtonHandler("ticket:create")
        void button() {}

        @ModalHandler("ticket:create")
        void submit(@Field("subject") String subject, @Field("details") Optional<String> details) {}
    }

    static final class BrokenModule {
        @ButtonHandler("ticket:create")
        void button(@Field("subject") String subject) {}
    }
}
