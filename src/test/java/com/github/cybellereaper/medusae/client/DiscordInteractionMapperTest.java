package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.commands.core.model.CommandType;
import com.github.cybellereaper.medusae.commands.core.model.InteractionHandlerType;
import com.github.cybellereaper.medusae.commands.discord.adapter.DiscordInteractionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiscordInteractionMapperTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void rejectsNullInputs() {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        assertThrows(NullPointerException.class, () -> mapper.toCoreInteraction(null, null));
    }

    @Test
    void mapsAllOfficialOptionTypesAndResolvedEntries() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = JSON.readTree("""
                {
                  "id":"1",
                  "token":"t",
                  "type":2,
                  "data":{
                    "name":"all-options",
                    "type":1,
                    "resolved":{
                      "users":{"42":{"id":"42","username":"neo"}},
                      "members":{"42":{"nick":"The One"}},
                      "roles":{"99":{"id":"99","name":"admin","permissions":"8"}},
                      "channels":{"77":{"id":"77","name":"general","type":0,"permissions":"0"}},
                      "messages":{"55":{"id":"55","content":"hello","author":{"id":"42"}}},
                      "attachments":{"66":{"id":"66","filename":"file.txt","size":2,"url":"u","proxy_url":"p"}}
                    },
                    "options":[
                      {"name":"text","type":3,"value":"abc"},
                      {"name":"count","type":4,"value":5},
                      {"name":"enabled","type":5,"value":true},
                      {"name":"target_user","type":6,"value":"42"},
                      {"name":"target_channel","type":7,"value":"77"},
                      {"name":"target_role","type":8,"value":"99"},
                      {"name":"target_mentionable","type":9,"value":"42"},
                      {"name":"ratio","type":10,"value":2.5},
                      {"name":"target_attachment","type":11,"value":"66"}
                    ]
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(node, (id, token, type, data) -> {});
        var interaction = mapper.toCoreInteraction(node, context);

        assertEquals("abc", interaction.options().get("text").value());
        assertEquals(5L, interaction.options().get("count").value());
        assertEquals(true, interaction.options().get("enabled").value());
        assertEquals("42", interaction.options().get("target_user").value());
        assertEquals("77", interaction.options().get("target_channel").value());
        assertEquals("99", interaction.options().get("target_role").value());
        assertEquals("42", interaction.options().get("target_mentionable").value());
        assertEquals(2.5D, interaction.options().get("ratio").value());
        assertEquals("66", interaction.options().get("target_attachment").value());

        assertNotNull(interaction.optionUsers().get("target_user"));
        assertNotNull(interaction.optionMembers().get("target_user"));
        assertNotNull(interaction.optionChannels().get("target_channel"));
        assertNotNull(interaction.optionRoles().get("target_role"));
        assertNotNull(interaction.optionUsers().get("target_mentionable"));
        assertNotNull(interaction.optionMembers().get("target_mentionable"));
        assertNotNull(interaction.optionAttachments().get("target_attachment"));

        assertEquals("neo", interaction.resolved().users().get("42").username());
        assertEquals("The One", interaction.resolved().members().get("42").nickname());
        assertEquals("admin", interaction.resolved().roles().get("99").name());
        assertEquals("general", interaction.resolved().channels().get("77").name());
        assertEquals("hello", interaction.resolved().messages().get("55").content());
        assertEquals("file.txt", interaction.resolved().attachments().get("66").filename());
    }

    @Test
    void supportsSubcommandAndSubcommandGroupOptionTypes() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = JSON.readTree("""
                {
                  "id":"1",
                  "token":"t",
                  "type":2,
                  "data":{
                    "name":"admin",
                    "type":1,
                    "options":[
                      {
                        "name":"group",
                        "type":2,
                        "options":[
                          {
                            "name":"ban",
                            "type":1,
                            "options":[{"name":"reason","type":3,"value":"spam","focused":true}]
                          }
                        ]
                      }
                    ]
                  }
                }
                """);

        InteractionContext context = InteractionContext.from(node, (id, token, type, data) -> {});
        var interaction = mapper.toCoreInteraction(node, context);

        assertEquals("group", interaction.subcommandGroup());
        assertEquals("ban", interaction.subcommand());
        assertEquals("reason", interaction.focusedOption());
        assertEquals("spam", interaction.options().get("reason").value());
    }

    @Test
    void mapsContextTargetsUsingResolvedMessagesUsersAndMembers() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var userContext = JSON.readTree("""
                {
                  "id":"1","token":"t","type":2,
                  "data":{
                    "name":"inspect","type":2,"target_id":"42",
                    "resolved":{"users":{"42":{"id":"42","username":"neo"}},"members":{"42":{"nick":"N"}}}
                  }
                }
                """);
        var messageContext = JSON.readTree("""
                {
                  "id":"1","token":"t","type":2,
                  "data":{
                    "name":"inspect-message","type":3,"target_id":"55",
                    "resolved":{"messages":{"55":{"id":"55","content":"hello","author":{"id":"42"}}}}
                  }
                }
                """);

        var userInteraction = mapper.toCoreInteraction(userContext, InteractionContext.from(userContext, (id, token, type, data) -> {}));
        var messageInteraction = mapper.toCoreInteraction(messageContext, InteractionContext.from(messageContext, (id, token, type, data) -> {}));

        assertEquals(CommandType.USER_CONTEXT, userInteraction.commandType());
        assertEquals("neo", userInteraction.targetUser().username());
        assertEquals("N", userInteraction.targetMember().nickname());

        assertEquals(CommandType.MESSAGE_CONTEXT, messageInteraction.commandType());
        assertEquals("55", messageInteraction.targetMessage().id());
    }

    @Test
    void handlesMissingOrPartialResolvedSectionsSafely() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = JSON.readTree("""
                {
                  "id":"1",
                  "token":"t",
                  "type":2,
                  "data":{
                    "name":"safe",
                    "type":1,
                    "resolved":{
                      "channels":{"77":{"id":"77","name":"partial","type":0}},
                      "members":{"42":{"nick":"partial-member"}}
                    },
                    "options":[
                      {"name":"target_channel","type":7,"value":"77"},
                      {"name":"target_user","type":6,"value":"42"}
                    ]
                  }
                }
                """);

        var interaction = mapper.toCoreInteraction(node, InteractionContext.from(node, (id, token, type, data) -> {}));

        assertEquals("partial", interaction.optionChannels().get("target_channel").name());
        assertFalse(interaction.optionUsers().containsKey("target_user"));
        assertFalse(interaction.optionMembers().containsKey("target_user"), "member should require corresponding user per Discord contract");
        assertTrue(interaction.resolved().members().isEmpty());
    }

    @Test
    void carriesResolvedDataForComponentAndModalInteractions() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var component = JSON.readTree("""
                {
                  "id":"9",
                  "token":"tt",
                  "type":3,
                  "data":{
                    "custom_id":"pick_role|state",
                    "component_type":8,
                    "resolved":{
                      "roles":{"99":{"id":"99","name":"admin","permissions":"8"}},
                      "channels":{"77":{"id":"77","name":"general","type":0,"permissions":"0"}}
                    }
                  }
                }
                """);

        var modal = JSON.readTree("""
                {
                  "id":"10",
                  "token":"tt",
                  "type":5,
                  "data":{
                    "custom_id":"feedback|state",
                    "components":[{"components":[{"custom_id":"body","value":"great"}]}],
                    "resolved":{
                      "attachments":{"66":{"id":"66","filename":"report.txt","size":1,"url":"u","proxy_url":"p"}},
                      "messages":{"55":{"id":"55","content":"source","author":{"id":"42"}}}
                    }
                  }
                }
                """);

        var componentExecution = mapper.toComponentInteraction(component, InteractionContext.from(component, (id, token, type, data) -> {}), InteractionHandlerType.CHANNEL_SELECT);
        var modalExecution = mapper.toModalInteraction(modal, InteractionContext.from(modal, (id, token, type, data) -> {}));

        assertEquals("admin", componentExecution.resolved().roles().get("99").name());
        assertEquals("general", componentExecution.resolved().channels().get("77").name());
        assertEquals("state", componentExecution.statePayload());

        assertEquals("great", modalExecution.modalFields().get("body"));
        assertEquals("report.txt", modalExecution.resolved().attachments().get("66").filename());
        assertEquals("source", modalExecution.resolved().messages().get("55").content());
    }

    @Test
    void handlesNullDataSafely() throws Exception {
        DiscordInteractionMapper mapper = new DiscordInteractionMapper();
        var node = JSON.readTree("""
                {"id":"1","token":"t","type":2}
                """);
        InteractionContext context = InteractionContext.from(node, (id, token, type, data) -> {});

        var interaction = mapper.toCoreInteraction(node, context);

        assertEquals("", interaction.commandName());
        assertTrue(interaction.options().isEmpty());
        assertTrue(interaction.resolved().users().isEmpty());
    }
}
