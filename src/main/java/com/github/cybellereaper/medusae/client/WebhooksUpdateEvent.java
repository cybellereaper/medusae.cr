package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhooksUpdateEvent(
        @JsonProperty("guild_id") String guildId,
        @JsonProperty("channel_id") String channelId
) {
}
