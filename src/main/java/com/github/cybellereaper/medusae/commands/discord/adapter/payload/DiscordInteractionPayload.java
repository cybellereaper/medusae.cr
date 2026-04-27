package com.github.cybellereaper.medusae.commands.discord.adapter.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.cybellereaper.medusae.client.ResolvedAttachment;
import com.github.cybellereaper.medusae.client.ResolvedChannel;
import com.github.cybellereaper.medusae.client.ResolvedMember;
import com.github.cybellereaper.medusae.client.ResolvedMessage;
import com.github.cybellereaper.medusae.client.ResolvedRole;
import com.github.cybellereaper.medusae.client.ResolvedUser;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordInteractionPayload(
        String id,
        String token,
        Integer type,
        @JsonProperty("guild_id") String guildId,
        @JsonProperty("channel_id") String channelId,
        Member member,
        ResolvedUser user,
        Data data
) {
    public int typeOrZero() {
        return type == null ? 0 : type;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Member(
            ResolvedUser user
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String name,
            Integer type,
            @JsonProperty("target_id") String targetId,
            @JsonProperty("component_type") Integer componentType,
            @JsonProperty("custom_id") String customId,
            List<Option> options,
            Resolved resolved,
            List<ActionRow> components
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(
            String name,
            Integer type,
            DiscordOptionValue value,
            Boolean focused,
            List<Option> options,
            Resolved resolved
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ActionRow(
            List<Component> components
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Component(
            @JsonProperty("custom_id") String customId,
            String value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resolved(
            Map<String, ResolvedUser> users,
            Map<String, ResolvedMember> members,
            Map<String, ResolvedChannel> channels,
            Map<String, ResolvedRole> roles,
            Map<String, ResolvedAttachment> attachments,
            Map<String, ResolvedMessage> messages
    ) {
    }
}
