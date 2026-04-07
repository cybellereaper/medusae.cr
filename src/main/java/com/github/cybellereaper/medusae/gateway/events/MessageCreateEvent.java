package com.github.cybellereaper.medusae.gateway.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageCreateEvent(
        String id,
        @JsonProperty("channel_id") String channelId,
        @JsonProperty("guild_id") String guildId,
        String content,
        Author author
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
            String id,
            String username,
            String discriminator
    ) {
    }
}
