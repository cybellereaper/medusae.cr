package com.github.cybellereaper.medusae.client;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

final class InteractionResponderFacade {
    private static final int RESPONSE_MESSAGE = 4;
    private static final int RESPONSE_DEFER_MESSAGE = 5;
    private static final int RESPONSE_DEFER_UPDATE = 6;
    private static final int RESPONSE_UPDATE_MESSAGE = 7;
    private static final int RESPONSE_AUTOCOMPLETE = 8;
    private static final int RESPONSE_MODAL = 9;
    private static final int MAX_AUTOCOMPLETE_CHOICES = 25;

    private final Supplier<String> interactionIdSupplier;
    private final Supplier<String> interactionTokenSupplier;
    private final SlashCommandRouter.InteractionResponder responder;

    InteractionResponderFacade(
            Supplier<String> interactionIdSupplier,
            Supplier<String> interactionTokenSupplier,
            SlashCommandRouter.InteractionResponder responder
    ) {
        this.interactionIdSupplier = Objects.requireNonNull(interactionIdSupplier, "interactionIdSupplier");
        this.interactionTokenSupplier = Objects.requireNonNull(interactionTokenSupplier, "interactionTokenSupplier");
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    void respondWithMessage(DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(RESPONSE_MESSAGE, message.toPayload());
    }

    void respondWithUpdatedMessage(DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(RESPONSE_UPDATE_MESSAGE, message.toPayload());
    }

    void respondWithModal(DiscordModal modal) {
        Objects.requireNonNull(modal, "modal");
        respond(RESPONSE_MODAL, modal.toPayload());
    }

    void respondWithAutocompleteChoices(List<AutocompleteChoice> choices) {
        Objects.requireNonNull(choices, "choices");
        if (choices.size() > MAX_AUTOCOMPLETE_CHOICES) {
            throw new IllegalArgumentException("choices must contain at most " + MAX_AUTOCOMPLETE_CHOICES + " entries");
        }
        respond(RESPONSE_AUTOCOMPLETE, Map.of("choices", choices.stream().map(AutocompleteChoice::toPayload).toList()));
    }

    void deferMessage() {
        respond(RESPONSE_DEFER_MESSAGE, null);
    }

    void deferUpdate() {
        respond(RESPONSE_DEFER_UPDATE, null);
    }

    private void respond(int type, Map<String, Object> data) {
        String interactionId = interactionIdSupplier.get();
        String interactionToken = interactionTokenSupplier.get();
        if (interactionId.isBlank() || interactionToken.isBlank()) {
            throw new IllegalArgumentException("interaction must include id and token");
        }
        responder.respond(interactionId, interactionToken, type, data);
    }
}
