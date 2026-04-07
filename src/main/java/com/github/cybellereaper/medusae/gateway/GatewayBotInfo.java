package com.github.cybellereaper.medusae.gateway;

public record GatewayBotInfo(
        String url,
        int shards,
        SessionStartLimit sessionStartLimit
) {
    public record SessionStartLimit(
            int total,
            int remaining,
            long resetAfter,
            int maxConcurrency
    ) {
    }
}
