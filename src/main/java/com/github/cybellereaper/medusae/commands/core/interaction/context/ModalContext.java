package com.github.cybellereaper.medusae.commands.core.interaction.context;

import com.github.cybellereaper.medusae.commands.core.execute.CommandResponder;
import com.github.cybellereaper.medusae.commands.core.model.InteractionExecution;

import java.util.Map;

public final class ModalContext extends InteractionContext {
    public ModalContext(InteractionExecution interaction, CommandResponder responder, Map<String, String> pathParams) {
        super(interaction, responder, pathParams);
    }

    public String field(String fieldId) {
        return interaction().modalFields().get(fieldId);
    }
}
