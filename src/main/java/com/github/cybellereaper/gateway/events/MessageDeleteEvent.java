package com.github.cybellereaper.gateway.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MessageDeleteEvent(
        String id,
        @JsonProperty("channel_id") String channelId,
        @JsonProperty("guild_id") String guildId
) {
}
