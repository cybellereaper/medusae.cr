package com.github.cybellereaper.medusae.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscordComponentsTest {
    @Test
    void messagePayloadIncludesButtonsAndSelectMenus() {
        DiscordActionRow buttonRow = DiscordActionRow.of(List.of(
                DiscordButton.primary("confirm", "Confirm"),
                DiscordButton.link("https://discord.com", "Open Docs")
        ));

        DiscordActionRow selectRow = DiscordActionRow.of(List.of(
                DiscordStringSelectMenu.of("theme", List.of(
                                DiscordSelectOption.of("Light", "light"),
                                DiscordSelectOption.of("Dark", "dark").asDefault()
                        ))
                        .withPlaceholder("Select theme")
                        .withSelectionRange(1, 1)
        ));

        Map<String, Object> payload = DiscordMessage.ofContent("Choose one")
                .withComponents(List.of(buttonRow, selectRow))
                .toPayload();

        assertTrue(payload.containsKey("components"));
        List<?> components = (List<?>) payload.get("components");
        assertEquals(2, components.size());
    }

    @Test
    void buttonValidationRejectsInvalidLinkButton() {
        assertThrows(IllegalArgumentException.class,
                () -> new DiscordButton(DiscordButton.LINK, "Docs", "id", null, null, false));
    }

    @Test
    void selectMenuValidationRejectsEmptyOptions() {
        assertThrows(IllegalArgumentException.class,
                () -> DiscordStringSelectMenu.of("theme", List.of()));
    }

    @Test
    void modalPayloadIncludesTextInputs() {
        DiscordModal modal = DiscordModal.of(
                "feedback_modal",
                "Feedback",
                List.of(DiscordActionRow.of(List.of(
                        DiscordTextInput.shortInput("summary", "Summary")
                                .withLengthRange(1, 100)
                                .withPlaceholder("Share quick feedback")
                )))
        );

        Map<String, Object> payload = modal.toPayload();

        assertEquals("feedback_modal", payload.get("custom_id"));
        assertTrue(payload.containsKey("components"));
    }

    @Test
    void modalValidationRejectsNonTextInputComponents() {
        assertThrows(IllegalArgumentException.class,
                () -> DiscordModal.of("id", "Title", List.of(
                        DiscordActionRow.of(List.of(DiscordButton.primary("a", "b")))
                )));
    }
}
