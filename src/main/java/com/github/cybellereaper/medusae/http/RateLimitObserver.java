package com.github.cybellereaper.medusae.http;

import java.time.Duration;

public interface RateLimitObserver {
    RateLimitObserver NOOP = new RateLimitObserver() {
    };

    default void onBucketBlocked(String bucketId, Duration duration, String source) {
    }

    default void onRateLimitedResponse(String bucketId, Duration retryAfter, boolean global, int statusCode) {
    }

    default void onRetryScheduled(String method, String path, int attempt, Duration backoff, String reason) {
    }

    default void onRequestCompleted(String method, String path, int attempts, int statusCode, Duration duration) {
    }
}
