package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cybellereaper.medusae.commands.core.resolve.ConversionSupport;

import java.util.List;
import java.util.Objects;

public final class InteractionContext {
    private static final String ID_FIELD = "id";
    private static final String TOKEN_FIELD = "token";
    private static final String TYPE_FIELD = "type";
    private static final String DATA_FIELD = "data";
    private static final String GUILD_ID_FIELD = "guild_id";
    private static final String CHANNEL_ID_FIELD = "channel_id";
    private static final String MEMBER_FIELD = "member";
    private static final String USER_FIELD = "user";
    private static final String CUSTOM_ID_FIELD = "custom_id";
    private static final String COMPONENTS_FIELD = "components";
    private static final String VALUE_FIELD = "value";

    private final JsonNode interaction;
    private final JsonNode data;
    private final InteractionOptionReader optionReader;
    private final ResolvedEntityReader resolvedEntityReader;
    private final InteractionResponderFacade responderFacade;

    private InteractionContext(JsonNode interaction, SlashCommandRouter.InteractionResponder responder) {
        this.interaction = Objects.requireNonNull(interaction, "interaction");
        this.data = interaction.path(DATA_FIELD);
        this.optionReader = new InteractionOptionReader(data.path(InteractionOptionReader.OPTIONS_FIELD));
        this.resolvedEntityReader = new ResolvedEntityReader(data);
        this.responderFacade = new InteractionResponderFacade(this::id, this::token, Objects.requireNonNull(responder, "responder"));
    }

    static InteractionContext from(JsonNode interaction, SlashCommandRouter.InteractionResponder responder) {
        return new InteractionContext(interaction, responder);
    }

    public JsonNode raw() {
        return interaction;
    }

    public String id() {
        return interaction.path(ID_FIELD).asText("");
    }

    public String token() {
        return interaction.path(TOKEN_FIELD).asText("");
    }

    public String commandName() {
        return InteractionOptionReader.textOrNull(data.path("name"));
    }

    public int interactionType() {
        return interaction.path(TYPE_FIELD).asInt(0);
    }

    public int commandType() {
        return data.path(TYPE_FIELD).asInt(0);
    }

    public String guildId() {
        return InteractionOptionReader.textOrNull(interaction.path(GUILD_ID_FIELD));
    }

    public String channelId() {
        return InteractionOptionReader.textOrNull(interaction.path(CHANNEL_ID_FIELD));
    }

    public String userId() {
        String memberUserId = InteractionOptionReader.textOrNull(interaction.path(MEMBER_FIELD).path(USER_FIELD).path(ID_FIELD));
        return memberUserId != null ? memberUserId : InteractionOptionReader.textOrNull(interaction.path(USER_FIELD).path(ID_FIELD));
    }

    public String customId() {
        return InteractionOptionReader.textOrNull(data.path(CUSTOM_ID_FIELD));
    }

    public String optionString(String optionName) {
        return optionReader.optionString(optionName);
    }

    public String requiredOptionString(String optionName) {
        String value = optionString(optionName);
        if (value == null) {
            throw new IllegalArgumentException("Missing required option: " + optionName);
        }
        return value;
    }

    public Long optionLong(String optionName) {
        return optionReader.optionLong(optionName);
    }

    public Integer optionInt(String optionName) {
        return optionReader.optionInt(optionName);
    }

    public Boolean optionBoolean(String optionName) {
        return optionReader.optionBoolean(optionName);
    }

    public Double optionDouble(String optionName) {
        return optionReader.optionDouble(optionName);
    }

    public JsonNode resolvedAttachment(String attachmentId) {
        return resolvedEntityReader.attachment(attachmentId);
    }

    public JsonNode resolvedUser(String userId) {
        return resolvedEntityReader.user(userId);
    }

    public JsonNode resolvedMember(String userId) {
        return resolvedEntityReader.member(userId);
    }

    public JsonNode resolvedRole(String roleId) {
        return resolvedEntityReader.role(roleId);
    }

    public JsonNode resolvedChannel(String channelId) {
        return resolvedEntityReader.channel(channelId);
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
        return resolvedEntityReader.attachmentValue(attachmentId);
    }

    public ResolvedUser resolvedUserValue(String userId) {
        return resolvedEntityReader.userValue(userId);
    }

    public ResolvedMember resolvedMemberValue(String userId) {
        return resolvedEntityReader.memberValue(userId);
    }

    public ResolvedRole resolvedRoleValue(String roleId) {
        return resolvedEntityReader.roleValue(roleId);
    }

    public ResolvedChannel resolvedChannelValue(String channelId) {
        return resolvedEntityReader.channelValue(channelId);
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

        JsonNode rows = data.path(COMPONENTS_FIELD);
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
                    return InteractionOptionReader.textOrNull(component.path(VALUE_FIELD));
                }
            }
        }
        return null;
    }

    public void respondWithMessage(String content) {
        respondWithMessage(DiscordMessage.ofContent(content));
    }

    public void respondWithMessage(DiscordMessage message) {
        responderFacade.respondWithMessage(message);
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
        responderFacade.respondWithModal(modal);
    }

    public void respondWithAutocompleteChoices(List<AutocompleteChoice> choices) {
        responderFacade.respondWithAutocompleteChoices(choices);
    }

    public void deferMessage() {
        responderFacade.deferMessage();
    }

    public void deferUpdate() {
        responderFacade.deferUpdate();
    }

    private String optionResolvedId(String optionName) {
        return optionReader.optionResolvedId(optionName);
    }
}
