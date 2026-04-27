package com.github.cybellereaper.medusae.http;

import java.net.http.HttpHeaders;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

public final class RateLimitManager {
    private final Map<String, Instant> blockedUntilByBucket = new ConcurrentHashMap<>();
    private final RateLimitObserver observer;

    public RateLimitManager() {
        this(RateLimitObserver.NOOP);
    }

    public RateLimitManager(RateLimitObserver observer) {
        this.observer = Objects.requireNonNull(observer, "observer");
    }

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
            blockFor(bucketId, resetAfterSeconds, "headers");
        }
    }

    public double updateFrom429(String bucketId, RateLimitErrorBody body) {
        double retryAfterSeconds = body == null || body.retryAfter() == null ? 1.0d : body.retryAfter();
        boolean global = body != null && Boolean.TRUE.equals(body.global());
        Duration retryAfter = Duration.ofMillis(Math.max(1, Math.round(retryAfterSeconds * 1000)));
        observer.onRateLimitedResponse(bucketId, retryAfter, global, 429);
        blockFor(bucketId, retryAfterSeconds, "429");
        return retryAfterSeconds;
    }

    private void blockFor(String bucketId, double seconds, String source) {
        long millis = Math.max(1L, Math.round(seconds * 1000));
        blockedUntilByBucket.put(bucketId, Instant.now().plusMillis(millis));
        observer.onBucketBlocked(bucketId, Duration.ofMillis(millis), source);
    }
}
