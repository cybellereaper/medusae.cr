package com.github.cybellereaper.http;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public record RetryPolicy(
        int maxAttempts,
        Duration baseDelay,
        Duration maxDelay,
        double jitterFactor
) {
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        Objects.requireNonNull(baseDelay, "baseDelay");
        Objects.requireNonNull(maxDelay, "maxDelay");
        if (baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("baseDelay must be > 0");
        }
        if (maxDelay.isNegative() || maxDelay.isZero()) {
            throw new IllegalArgumentException("maxDelay must be > 0");
        }
        if (maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("maxDelay must be >= baseDelay");
        }
        if (jitterFactor < 0 || jitterFactor > 1) {
            throw new IllegalArgumentException("jitterFactor must be between 0 and 1");
        }
    }

    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(4, Duration.ofMillis(250), Duration.ofSeconds(5), 0.2);
    }

    public boolean shouldRetryStatus(int statusCode) {
        return statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500;
    }

    public Duration delayForAttempt(int attempt) {
        long shift = Math.min(30, Math.max(0, attempt - 1));
        long multiplier = 1L << shift;
        long rawMillis = baseDelay.toMillis() * multiplier;
        long cappedMillis = Math.min(rawMillis, maxDelay.toMillis());

        if (jitterFactor == 0) {
            return Duration.ofMillis(cappedMillis);
        }

        double jitterWindow = cappedMillis * jitterFactor;
        long min = Math.max(1L, Math.round(cappedMillis - jitterWindow));
        long max = Math.max(min, Math.round(cappedMillis + jitterWindow));
        long jittered = ThreadLocalRandom.current().nextLong(min, max + 1);
        return Duration.ofMillis(jittered);
    }
}
