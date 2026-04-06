package com.github.cybellereaper.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.commands.discord.adapter.DiscordInteractionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscordInteractionMapperTest {
    @Test
    void rejectsNullInputs() {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        assertThrows(NullPointerException.class, () -> mapper.toCoreInteraction(null, null));
    }

    @Test
    void handlesMissingTargetIdWithoutNpe() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = new ObjectMapper().readTree("""
                {
                  "id":"1",
                  "token":"t",
                  "type":2,
                  "data":{
                    "name":"inspect user",
                    "type":2
                  }
                }
                """);
        InteractionContext context = InteractionContext.from(node, (id, token, type, data) -> {});

        assertDoesNotThrow(() -> mapper.toCoreInteraction(node, context));
    }

    @Test
    void mapsResolvedUserAndMemberForSlashOptionsUsingDataResolved() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = new ObjectMapper().readTree("""
                {
                  "id":"1",
                  "token":"t",
                  "type":2,
                  "data":{
                    "name":"user",
                    "type":1,
                    "resolved":{
                      "users":{"42":{"id":"42","username":"tester"}},
                      "members":{"42":{"nick":"ModTarget"}}
                    },
                    "options":[
                      {
                        "name":"target",
                        "type":6,
                        "value":"42"
                      }
                    ]
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(node, (id, token, type, data) -> {});
        var interaction = mapper.toCoreInteraction(node, context);

        ResolvedUser user = (ResolvedUser) interaction.optionUsers().get("target");
        ResolvedMember member = (ResolvedMember) interaction.optionMembers().get("target");

        assertNotNull(user);
        assertEquals("tester", user.username());
        assertNotNull(member);
        assertEquals("42", member.userId());
    }

    @Test
    void ignoresMissingResolvedMemberInsteadOfCrashing() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = new ObjectMapper().readTree("""
                {
                  "id":"1",
                  "token":"t",
                  "type":2,
                  "data":{
                    "name":"user",
                    "type":1,
                    "resolved":{
                      "users":{"42":{"id":"42","username":"tester"}}
                    },
                    "options":[
                      {
                        "name":"target",
                        "type":6,
                        "value":"42"
                      }
                    ]
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(node, (id, token, type, data) -> {});

        assertDoesNotThrow(() -> {
            var interaction = mapper.toCoreInteraction(node, context);
            assertNotNull(interaction.optionUsers().get("target"));
            assertFalse(interaction.optionMembers().containsKey("target"));
        });
    }
}
