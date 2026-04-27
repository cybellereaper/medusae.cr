package com.github.cybellereaper.medusae.client;

import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;

import java.util.List;
import java.util.Objects;

public final class InteractionContext {
    private final DiscordInteractionPayload interaction;
    private final InteractionOptionReader optionReader;
    private final ResolvedEntityReader resolvedEntityReader;
    private final InteractionResponderFacade responderFacade;

    private InteractionContext(DiscordInteractionPayload interaction, SlashCommandRouter.InteractionResponder responder) {
        this.interaction = Objects.requireNonNull(interaction, "interaction");
        DiscordInteractionPayload.Data data = interaction.data();
        this.optionReader = new InteractionOptionReader(data == null ? null : data.options());
        this.resolvedEntityReader = new ResolvedEntityReader(data == null ? null : data.resolved());
        this.responderFacade = new InteractionResponderFacade(this::id, this::token, Objects.requireNonNull(responder, "responder"));
    }

    static InteractionContext from(DiscordInteractionPayload interaction, SlashCommandRouter.InteractionResponder responder) {
        return new InteractionContext(interaction, responder);
    }

    public DiscordInteractionPayload raw() {
        return interaction;
    }

    public String id() {
        return stringOrEmpty(interaction.id());
    }

    public String token() {
        return stringOrEmpty(interaction.token());
    }

    public String commandName() {
        return interaction.data() == null ? null : textOrNull(interaction.data().name());
    }

    public int interactionType() {
        return interaction.typeOrZero();
    }

    public int commandType() {
        return interaction.data() == null || interaction.data().type() == null ? 0 : interaction.data().type();
    }

    public String guildId() {
        return textOrNull(interaction.guildId());
    }

    public String channelId() {
        return textOrNull(interaction.channelId());
    }

    public String userId() {
        ResolvedUser memberUser = interaction.member() == null ? null : interaction.member().user();
        String memberUserId = memberUser == null ? null : textOrNull(memberUser.id());
        return memberUserId != null ? memberUserId : interaction.user() == null ? null : textOrNull(interaction.user().id());
    }

    public String customId() {
        return interaction.data() == null ? null : textOrNull(interaction.data().customId());
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
        DiscordInteractionPayload.Data data = interaction.data();
        if (data == null || data.components() == null) {
            return null;
        }

        for (DiscordInteractionPayload.ActionRow row : data.components()) {
            if (row.components() == null) {
                continue;
            }
            for (DiscordInteractionPayload.Component component : row.components()) {
                if (customId.equals(component.customId())) {
                    return textOrNull(component.value());
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

    public void respondWithUpdatedMessage(DiscordMessage message) {
        responderFacade.respondWithUpdatedMessage(message);
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

    private static String textOrNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static String stringOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
