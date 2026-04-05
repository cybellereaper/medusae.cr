package com.github.cybellereaper.gateway.events;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InteractionCreateEvent(
        String id,
        Integer type,
        String token,
        @JsonProperty("guild_id") String guildId,
        @JsonProperty("channel_id") String channelId,
        Data data,
        Member member,
        User user
) {
    public record Data(
            String id,
            String name,
            @JsonProperty("custom_id") String customId
    ) {
    }

    public record Member(
            User user,
            String nick
    ) {
    }

    public record User(
            String id,
            String username,
            String discriminator
    ) {
    }
}
