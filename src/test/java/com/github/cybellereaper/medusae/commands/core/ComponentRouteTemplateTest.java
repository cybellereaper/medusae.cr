package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.commands.core.exception.RegistrationException;
import com.github.cybellereaper.medusae.commands.core.interaction.route.ComponentRouteTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRouteTemplateTest {

    @Test
    void matchesTemplateAndExtractsParamsAndState() {
        ComponentRouteTemplate template = ComponentRouteTemplate.compile("ticket:close:{ticketId}");
        var match = template.match("ticket:close:42|sig").orElseThrow();

        assertEquals("42", match.pathParams().get("ticketid"));
        assertEquals("sig", match.statePayload());
    }

    @Test
    void failsOnDuplicateParamNames() {
        assertThrows(RegistrationException.class, () -> ComponentRouteTemplate.compile("x:{id}:{id}"));
    }
}
