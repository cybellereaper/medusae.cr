package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InteractionContext {
    private static final String DATA_FIELD = "data";
    private static final String OPTIONS_FIELD = "options";
    private static final String COMPONENTS_FIELD = "components";
    private static final String NAME_FIELD = "name";
    private static final String VALUE_FIELD = "value";
    private static final String CUSTOM_ID_FIELD = "custom_id";
    private static final String TYPE_FIELD = "type";
    private static final String RESOLVED_FIELD = "resolved";
    private static final int MAX_AUTOCOMPLETE_CHOICES = 25;

    private final JsonNode interaction;
    private final SlashCommandRouter.InteractionResponder responder;

    private InteractionContext(JsonNode interaction, SlashCommandRouter.InteractionResponder responder) {
        this.interaction = Objects.requireNonNull(interaction, "interaction");
        this.responder = Objects.requireNonNull(responder, "responder");
    }

    static InteractionContext from(JsonNode interaction, SlashCommandRouter.InteractionResponder responder) {
        return new InteractionContext(interaction, responder);
    }

    private static JsonNode findOptionNode(String optionName, JsonNode options) {
        if (!options.isArray()) {
            return null;
        }
        for (JsonNode option : options) {
            if (optionName.equals(option.path(NAME_FIELD).asText())) {
                return option;
            }
            JsonNode nested = findOptionNode(optionName, option.path(OPTIONS_FIELD));
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    public JsonNode raw() {
        return interaction;
    }

    public String id() {
        return interaction.path("id").asText("");
    }

    public String token() {
        return interaction.path("token").asText("");
    }

    public String commandName() {
        return textOrNull(interaction.path(DATA_FIELD).path(NAME_FIELD));
    }

    public int interactionType() {
        return interaction.path(TYPE_FIELD).asInt(0);
    }

    public int commandType() {
        return interaction.path(DATA_FIELD).path(TYPE_FIELD).asInt(0);
    }

    public String guildId() {
        return textOrNull(interaction.path("guild_id"));
    }

    public String channelId() {
        return textOrNull(interaction.path("channel_id"));
    }

    public String userId() {
        String memberUserId = textOrNull(interaction.path("member").path("user").path("id"));
        return memberUserId != null ? memberUserId : textOrNull(interaction.path("user").path("id"));
    }

    public String customId() {
        return textOrNull(interaction.path(DATA_FIELD).path(CUSTOM_ID_FIELD));
    }

    public String optionString(String optionName) {
        Objects.requireNonNull(optionName, "optionName");
        JsonNode option = findOptionNode(optionName, interaction.path(DATA_FIELD).path(OPTIONS_FIELD));
        return option == null ? null : textOrNull(option.path(VALUE_FIELD));
    }

    public String requiredOptionString(String optionName) {
        String value = optionString(optionName);
        if (value == null) {
            throw new IllegalArgumentException("Missing required option: " + optionName);
        }
        return value;
    }

    public Long optionLong(String optionName) {
        JsonNode option = findOptionNode(optionName, interaction.path(DATA_FIELD).path(OPTIONS_FIELD));
        if (option == null) {
            return null;
        }
        JsonNode value = option.path(VALUE_FIELD);
        if (value.isIntegralNumber()) {
            return value.longValue();
        }
        if (value.isTextual()) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public Integer optionInt(String optionName) {
        Long value = optionLong(optionName);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    public Boolean optionBoolean(String optionName) {
        JsonNode option = findOptionNode(optionName, interaction.path(DATA_FIELD).path(OPTIONS_FIELD));
        if (option == null) {
            return null;
        }
        JsonNode value = option.path(VALUE_FIELD);
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            String text = value.asText().trim();
            if ("true".equalsIgnoreCase(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text)) {
                return false;
            }
        }
        return null;
    }

    public Double optionDouble(String optionName) {
        JsonNode option = findOptionNode(optionName, interaction.path(DATA_FIELD).path(OPTIONS_FIELD));
        if (option == null) {
            return null;
        }
        JsonNode value = option.path(VALUE_FIELD);
        if (value.isNumber()) {
            return value.doubleValue();
        }
        if (value.isTextual()) {
            try {
                return Double.parseDouble(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public JsonNode resolvedAttachment(String attachmentId) {
        return resolvedEntity("attachments", attachmentId);
    }

    public JsonNode resolvedUser(String userId) {
        return resolvedEntity("users", userId);
    }

    public JsonNode resolvedMember(String userId) {
        return resolvedEntity("members", userId);
    }

    public JsonNode resolvedRole(String roleId) {
        return resolvedEntity("roles", roleId);
    }

    public JsonNode resolvedChannel(String channelId) {
        return resolvedEntity("channels", channelId);
    }

    public JsonNode optionResolvedAttachment(String optionName) {
        return resolvedAttachment(optionResolvedId(optionName));
    }

    public JsonNode optionResolvedUser(String optionName) {
        return resolvedUser(optionResolvedId(optionName));
    }

    public JsonNode optionResolvedRole(String optionName) {
        return resolvedRole(optionResolvedId(optionName));
    }

    public JsonNode optionResolvedChannel(String optionName) {
        return resolvedChannel(optionResolvedId(optionName));
    }

    public ResolvedAttachment resolvedAttachmentValue(String attachmentId) {
        return ResolvedAttachment.from(resolvedAttachment(attachmentId));
    }

    public ResolvedUser resolvedUserValue(String userId) {
        return ResolvedUser.from(resolvedUser(userId));
    }

    public ResolvedMember resolvedMemberValue(String userId) {
        return ResolvedMember.from(userId, resolvedMember(userId));
    }

    public ResolvedRole resolvedRoleValue(String roleId) {
        return ResolvedRole.from(resolvedRole(roleId));
    }

    public ResolvedChannel resolvedChannelValue(String channelId) {
        return ResolvedChannel.from(resolvedChannel(channelId));
    }

    public ResolvedAttachment optionResolvedAttachmentValue(String optionName) {
        return resolvedAttachmentValue(optionResolvedId(optionName));
    }

    public ResolvedUser optionResolvedUserValue(String optionName) {
        return resolvedUserValue(optionResolvedId(optionName));
    }

    public ResolvedRole optionResolvedRoleValue(String optionName) {
        return resolvedRoleValue(optionResolvedId(optionName));
    }

    public ResolvedChannel optionResolvedChannelValue(String optionName) {
        return resolvedChannelValue(optionResolvedId(optionName));
    }

    public String modalValue(String customId) {
        Objects.requireNonNull(customId, "customId");

        JsonNode rows = interaction.path(DATA_FIELD).path(COMPONENTS_FIELD);
        if (!rows.isArray()) {
            return null;
        }

        for (JsonNode row : rows) {
            JsonNode components = row.path(COMPONENTS_FIELD);
            if (!components.isArray()) {
                continue;
            }
            for (JsonNode component : components) {
                if (customId.equals(component.path(CUSTOM_ID_FIELD).asText())) {
                    return textOrNull(component.path(VALUE_FIELD));
                }
            }
        }
        return null;
    }

    public void respondWithMessage(String content) {
        respondWithMessage(DiscordMessage.ofContent(content));
    }

    public void respondWithMessage(DiscordMessage message) {
        Objects.requireNonNull(message, "message");
        respond(4, message.toPayload());
    }

    public void respondWithEmbeds(String content, List<DiscordEmbed> embeds) {
        respondWithMessage(DiscordMessage.ofEmbeds(content, embeds));
    }

    public void respondEphemeral(String content) {
        respondWithMessage(DiscordMessage.ofContent(content).asEphemeral());
    }

    public void respondEphemeralWithEmbeds(String content, List<DiscordEmbed> embeds) {
        respondWithMessage(DiscordMessage.ofEmbeds(content, embeds).asEphemeral());
    }

    public void respondWithModal(DiscordModal modal) {
        Objects.requireNonNull(modal, "modal");
        respond(9, modal.toPayload());
    }

    public void respondWithAutocompleteChoices(List<AutocompleteChoice> choices) {
        Objects.requireNonNull(choices, "choices");
        if (choices.size() > MAX_AUTOCOMPLETE_CHOICES) {
            throw new IllegalArgumentException("choices must contain at most " + MAX_AUTOCOMPLETE_CHOICES + " entries");
        }
        respond(8, Map.of("choices", choices.stream().map(AutocompleteChoice::toPayload).toList()));
    }

    public void deferMessage() {
        respond(5, null);
    }

    public void deferUpdate() {
        respond(6, null);
    }

    private void respond(int type, Map<String, Object> data) {
        String interactionId = id();
        String interactionToken = token();
        if (interactionId.isBlank() || interactionToken.isBlank()) {
            throw new IllegalArgumentException("interaction must include id and token");
        }
        responder.respond(interactionId, interactionToken, type, data);
    }

    private String optionResolvedId(String optionName) {
        JsonNode option = findOptionNode(optionName, interaction.path(DATA_FIELD).path(OPTIONS_FIELD));
        if (option == null) {
            return null;
        }
        JsonNode value = option.path(VALUE_FIELD);
        if (value.isTextual()) {
            String text = value.asText().trim();
            return text.isEmpty() ? null : text;
        }
        if (value.isIntegralNumber()) {
            return Long.toString(value.longValue());
        }
        return null;
    }

    private JsonNode resolvedEntity(String entityType, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        JsonNode entity = interaction.path(DATA_FIELD).path(RESOLVED_FIELD).path(entityType).path(id);
        return entity.isMissingNode() || entity.isNull() ? null : entity;
    }
}
