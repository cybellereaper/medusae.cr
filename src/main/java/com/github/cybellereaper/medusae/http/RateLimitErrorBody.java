package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RateLimitErrorBody(
        @JsonProperty("retry_after") Double retryAfter,
        Boolean global
) {
    public double retryAfterOrDefault() {
        return retryAfter == null ? 1.0d : retryAfter;
    }

    public boolean globalOrFalse() {
        return Boolean.TRUE.equals(global);
    }
}
