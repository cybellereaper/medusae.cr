package com.github.cybellereaper.medusae.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

record GatewayResumePayload(
        String token,
        @JsonProperty("session_id") String sessionId,
        long seq
) {
}
