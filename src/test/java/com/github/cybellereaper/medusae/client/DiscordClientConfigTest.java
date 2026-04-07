package com.github.cybellereaper.medusae.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscordClientConfigTest {

    @Test
    void supportsShardingConfiguration() {
        DiscordClientConfig config = DiscordClientConfig.builder("token")
                .shard(2, 8)
                .build();

        assertEquals(2, config.shardId());
        assertEquals(8, config.shardCount());
    }

    @Test
    void rejectsInvalidShardRange() {
        assertThrows(IllegalArgumentException.class,
                () -> DiscordClientConfig.builder("token").shard(4, 4).build());
        assertThrows(IllegalArgumentException.class,
                () -> DiscordClientConfig.builder("token").shard(0, 0).build());
    }
}
