package com.github.cybellereaper.medusae.gateway.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReadyEvent(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("resume_gateway_url") String resumeGatewayUrl
) {
}
