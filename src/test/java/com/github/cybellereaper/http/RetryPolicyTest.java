package com.github.cybellereaper.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void retriesServerAndRateLimitStatuses() {
        RetryPolicy policy = RetryPolicy.defaultPolicy();
        assertTrue(policy.shouldRetryStatus(500));
        assertTrue(policy.shouldRetryStatus(429));
        assertFalse(policy.shouldRetryStatus(400));
    }

    @Test
    void appliesExponentialBackoffWithoutJitter() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(100), Duration.ofSeconds(1), 0);

        assertEquals(Duration.ofMillis(100), policy.delayForAttempt(1));
        assertEquals(Duration.ofMillis(200), policy.delayForAttempt(2));
        assertEquals(Duration.ofMillis(400), policy.delayForAttempt(3));
    }

    @Test
    void capsBackoffAtMaxDelay() {
        RetryPolicy policy = new RetryPolicy(5, Duration.ofMillis(500), Duration.ofMillis(600), 0);
        assertEquals(Duration.ofMillis(600), policy.delayForAttempt(3));
    }
}
