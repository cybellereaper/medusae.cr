package com.github.cybellereaper.medusae.commands.discord.adapter.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordInteractionPayload(
        Data data
) {
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
            JsonNode value,
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
            Map<String, JsonNode> users,
            Map<String, JsonNode> members,
            Map<String, JsonNode> channels,
            Map<String, JsonNode> roles,
            Map<String, JsonNode> attachments,
            Map<String, JsonNode> messages
    ) {
    }
}
