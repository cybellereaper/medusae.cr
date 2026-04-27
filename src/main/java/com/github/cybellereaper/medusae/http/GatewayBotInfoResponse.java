package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record GatewayBotInfoResponse(
        String url,
        int shards,
        @JsonProperty("session_start_limit") SessionStartLimit sessionStartLimit
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SessionStartLimit(
            int total,
            int remaining,
            @JsonProperty("reset_after") long resetAfter,
            @JsonProperty("max_concurrency") int maxConcurrency
    ) {
    }
}
