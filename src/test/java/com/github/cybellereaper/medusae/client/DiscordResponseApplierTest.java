package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.commands.core.response.FollowupResponse;
import com.github.cybellereaper.medusae.commands.core.response.InteractionReply;
import com.github.cybellereaper.medusae.commands.core.response.ModalReply;
import com.github.cybellereaper.medusae.commands.core.response.component.ActionRowSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.ButtonSpec;
import com.github.cybellereaper.medusae.commands.core.response.component.StringSelectSpec;
import com.github.cybellereaper.medusae.commands.discord.response.DiscordResponseApplier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscordResponseApplierTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void appliesDeferredUpdateResponse() {
        CapturingResponder responder = new CapturingResponder();
        InteractionContext context = context(responder);

        new DiscordResponseApplier(context).accept(InteractionReply.deferUpdate().build());

        assertEquals(6, responder.type);
        assertNull(responder.data);
    }

    @Test
    void appliesInteractionReplyWithMixedComponents() {
        CapturingResponder responder = new CapturingResponder();
        InteractionContext context = context(responder);
        InteractionReply response = InteractionReply.updateMessage()
                .content("Updated")
                .components(List.of(ActionRowSpec.of(
                        ButtonSpec.primary("save", "Save"),
                        StringSelectSpec.of("theme", List.of(
                                StringSelectSpec.Option.of("Light", "light"),
                                StringSelectSpec.Option.of("Dark", "dark")
                        ))
                )))
                .build();

        new DiscordResponseApplier(context).accept(response);

        assertEquals(7, responder.type);
        assertNotNull(responder.data);
        assertEquals("Updated", responder.data.get("content"));
        assertTrue(responder.data.containsKey("components"));
    }

    @Test
    void appliesModalReply() {
        CapturingResponder responder = new CapturingResponder();
        InteractionContext context = context(responder);
        ModalReply modalReply = ModalReply.create("ticket:create")
                .title("Create Ticket")
                .textInput("subject", "Subject", true)
                .build();

        new DiscordResponseApplier(context).accept(modalReply);

        assertEquals(9, responder.type);
        assertNotNull(responder.data);
        assertEquals("ticket:create", responder.data.get("custom_id"));
    }

    @Test
    void followupResponseRemainsUnsupported() {
        CapturingResponder responder = new CapturingResponder();
        InteractionContext context = context(responder);

        assertThrows(UnsupportedOperationException.class,
                () -> new DiscordResponseApplier(context).accept(new FollowupResponse("later", false)));
    }

    private static InteractionContext context(CapturingResponder responder) {
        return InteractionContext.from(OBJECT_MAPPER.valueToTree(Map.of("id", "123", "token", "abc")), responder);
    }

    private static final class CapturingResponder implements SlashCommandRouter.InteractionResponder {
        private int type;
        private Map<String, Object> data;

        @Override
        public void respond(String interactionId, String interactionToken, int type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }
    }
}
