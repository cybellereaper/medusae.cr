package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.commands.core.execute.CommandResponder;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ComponentContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.InteractionContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.ModalContext;
import com.github.cybellereaper.medusae.commands.core.interaction.context.SelectContext;
import com.github.cybellereaper.medusae.commands.core.model.InteractionExecution;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import com.github.cybellereaper.medusae.commands.core.model.ResolvedEntities;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class InteractionContextFactoryTest {

    private static final CommandResponder NOOP_RESPONDER = response -> {
    };

    @Test
    void createsComponentContextForButtonInteractions() {
        InteractionContext context = InteractionContext.from(interaction(InteractionHandlerType.BUTTON), NOOP_RESPONDER, Map.of("id", "42"));

        assertInstanceOf(ComponentContext.class, context);
        assertEquals("42", context.pathParam("id"));
    }

    @Test
    void createsModalContextForModalInteractions() {
        InteractionContext context = InteractionContext.from(interaction(InteractionHandlerType.MODAL), NOOP_RESPONDER, Map.of());

        assertInstanceOf(ModalContext.class, context);
    }

    @Test
    void createsSelectContextForAllSelectInteractionTypes() {
        assertSelectContext(InteractionHandlerType.STRING_SELECT);
        assertSelectContext(InteractionHandlerType.USER_SELECT);
        assertSelectContext(InteractionHandlerType.ROLE_SELECT);
        assertSelectContext(InteractionHandlerType.MENTIONABLE_SELECT);
        assertSelectContext(InteractionHandlerType.CHANNEL_SELECT);
    }

    private void assertSelectContext(InteractionHandlerType type) {
        InteractionContext context = InteractionContext.from(interaction(type), NOOP_RESPONDER, Map.of());
        assertInstanceOf(SelectContext.class, context, () -> "Expected SelectContext for " + type);
    }

    private static InteractionExecution interaction(InteractionHandlerType type) {
        return new InteractionExecution(
                type,
                "route",
                Map.of(),
                null,
                ResolvedEntities.empty(),
                true,
                null,
                "user-1",
                Set.of(),
                Set.of(),
                null
        );
    }
}
