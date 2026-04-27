package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuildMemberRemoveEvent(
        @JsonProperty("guild_id") String guildId,
        ResolvedUser user
) {
}
