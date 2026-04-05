package com.github.cybellereaper.gateway.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GuildCreateEvent(
        String id,
        String name,
        Boolean unavailable,
        @JsonProperty("member_count") Integer memberCount
) {
}
