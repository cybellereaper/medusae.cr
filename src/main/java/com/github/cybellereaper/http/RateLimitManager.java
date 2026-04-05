package com.github.cybellereaper.http;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.http.HttpHeaders;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

public final class RateLimitManager {
    private final Map<String, Instant> blockedUntilByBucket = new ConcurrentHashMap<>();

    public void await(String bucketId) {
        Instant blockedUntil = blockedUntilByBucket.get(bucketId);
        if (blockedUntil == null) {
            return;
        }

        long nanos = java.time.Duration.between(Instant.now(), blockedUntil).toNanos();
        if (nanos <= 0) {
            blockedUntilByBucket.remove(bucketId, blockedUntil);
            return;
        }

        LockSupport.parkNanos(nanos);
    }

    public void updateFromHeaders(String bucketId, HttpHeaders headers) {
        long remaining = headers.firstValueAsLong("X-RateLimit-Remaining").orElse(-1);
        double resetAfterSeconds = headers.firstValue("X-RateLimit-Reset-After")
                .map(Double::parseDouble)
                .orElse(0d);

        if (remaining == 0 && resetAfterSeconds > 0) {
            blockFor(bucketId, resetAfterSeconds);
        }
    }

    public void updateFrom429(String bucketId, JsonNode body) {
        double retryAfterSeconds = body.path("retry_after").asDouble(1.0d);
        blockFor(bucketId, retryAfterSeconds);
    }

    private void blockFor(String bucketId, double seconds) {
        long millis = Math.max(1L, Math.round(seconds * 1000));
        blockedUntilByBucket.put(bucketId, Instant.now().plusMillis(millis));
    }
}
