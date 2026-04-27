package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GuildMemberSnapshot(
        @JsonProperty("guild_id") String guildId,
        ResolvedUser user,
        @JsonProperty("nick") String nickname
) {
    public String userId() {
        return user == null ? null : user.id();
    }
}
