package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.DiscordClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordRestClientRetryTest {
    private final TestRateLimitObserver observer = new TestRateLimitObserver();

    @Test
    void retriesTransportExceptionsUpToMaxAttempts() {
        SequenceHttpClient httpClient = new SequenceHttpClient(
                () -> { throw new IOException("boom-1"); },
                () -> { throw new IOException("boom-2"); },
                () -> new FixedHttpResponse("{\"ok\":true}", 200, Map.of())
        );

        DiscordRestClient restClient = buildClient(httpClient, new RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(2), 0));

        assertEquals(Boolean.TRUE, restClient.request("GET", "/test", null).get("ok"));
        assertEquals(3, httpClient.sendCalls.get());
        assertEquals(2, observer.retries.size());
        assertEquals("transport", observer.retries.get(0).reason());
        assertEquals("transport", observer.retries.get(1).reason());
        assertEquals(1, observer.completions.size());
        assertEquals(3, observer.completions.get(0).attempts());
        assertEquals(200, observer.completions.get(0).statusCode());
    }

    @Test
    void retries429UsingRetryAfterValue() {
        SequenceHttpClient httpClient = new SequenceHttpClient(
                () -> new FixedHttpResponse("{\"retry_after\":0.001,\"global\":false}", 429, Map.of()),
                () -> new FixedHttpResponse("{\"ok\":true}", 200, Map.of())
        );

        DiscordRestClient restClient = buildClient(httpClient, new RetryPolicy(2, Duration.ofMillis(1), Duration.ofMillis(2), 0));

        assertEquals(Boolean.TRUE, restClient.request("GET", "/test", null).get("ok"));
        assertEquals(1, observer.retries.size());
        assertEquals("rate_limit", observer.retries.get(0).reason());
        assertEquals(Duration.ofMillis(1), observer.retries.get(0).backoff());
        assertEquals(1, observer.rateLimited.size());
        assertEquals(429, observer.rateLimited.get(0).statusCode());
        assertEquals(Duration.ofMillis(1), observer.rateLimited.get(0).retryAfter());
    }

    @Test
    void failsImmediatelyForNonRetryableStatus() {
        SequenceHttpClient httpClient = new SequenceHttpClient(
                () -> new FixedHttpResponse("{\"error\":\"bad_request\"}", 400, Map.of())
        );
        DiscordRestClient restClient = buildClient(httpClient, new RetryPolicy(4, Duration.ofMillis(1), Duration.ofMillis(2), 0));

        DiscordHttpException exception = assertThrows(
                DiscordHttpException.class,
                () -> restClient.request("GET", "/test", null)
        );

        assertEquals(400, exception.statusCode());
        assertEquals(1, httpClient.sendCalls.get());
        assertEquals(0, observer.retries.size());
        assertEquals(1, observer.completions.size());
        assertEquals(1, observer.completions.get(0).attempts());
        assertEquals(400, observer.completions.get(0).statusCode());
    }

    @AfterEach
    void clearObserverEvents() {
        observer.clear();
    }

    private DiscordRestClient buildClient(HttpClient httpClient, RetryPolicy retryPolicy) {
        return new DiscordRestClient(
                httpClient,
                new ObjectMapper(),
                DiscordClientConfig.builder("token")
                        .requestTimeout(Duration.ofSeconds(1))
                        .build(),
                retryPolicy,
                observer
        );
    }

    private static final class SequenceHttpClient extends HttpClient {
        private final Deque<ThrowingResponseStep> responses = new ArrayDeque<>();
        private final AtomicInteger sendCalls = new AtomicInteger();

        @SafeVarargs
        private SequenceHttpClient(ThrowingResponseStep... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            sendCalls.incrementAndGet();
            if (responses.isEmpty()) {
                throw new IllegalStateException("No more scripted responses");
            }
            FixedHttpResponse scripted = responses.removeFirst().get();
            @SuppressWarnings("unchecked")
            HttpResponse<T> cast = (HttpResponse<T>) scripted.withRequest(request);
            return cast;
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("Not needed for tests");
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("Not needed for tests");
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }
    }

    @FunctionalInterface
    private interface ThrowingResponseStep {
        FixedHttpResponse get() throws IOException, InterruptedException;
    }

    private record FixedHttpResponse(HttpRequest request, String body, int statusCode, Map<String, List<String>> rawHeaders)
            implements HttpResponse<String> {
        private FixedHttpResponse(String body, int statusCode, Map<String, List<String>> rawHeaders) {
            this(HttpRequest.newBuilder(URI.create("https://example.test")).GET().build(), body, statusCode, rawHeaders);
        }

        private FixedHttpResponse withRequest(HttpRequest updatedRequest) {
            return new FixedHttpResponse(updatedRequest, body, statusCode, rawHeaders);
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(rawHeaders, (a, b) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private static final class TestRateLimitObserver implements RateLimitObserver {
        private final List<RetryEvent> retries = new ArrayList<>();
        private final List<CompletionEvent> completions = new ArrayList<>();
        private final List<RateLimitedEvent> rateLimited = new ArrayList<>();

        @Override
        public void onRetryScheduled(String method, String path, int attempt, Duration backoff, String reason) {
            retries.add(new RetryEvent(method, path, attempt, backoff, reason));
        }

        @Override
        public void onRequestCompleted(String method, String path, int attempts, int statusCode, Duration duration) {
            completions.add(new CompletionEvent(method, path, attempts, statusCode, duration));
        }

        @Override
        public void onRateLimitedResponse(String bucketId, Duration retryAfter, boolean global, int statusCode) {
            rateLimited.add(new RateLimitedEvent(bucketId, retryAfter, global, statusCode));
        }

        private void clear() {
            retries.clear();
            completions.clear();
            rateLimited.clear();
        }
    }

    private record RetryEvent(String method, String path, int attempt, Duration backoff, String reason) {
    }

    private record CompletionEvent(String method, String path, int attempts, int statusCode, Duration duration) {
    }

    private record RateLimitedEvent(String bucketId, Duration retryAfter, boolean global, int statusCode) {
    }
}
