package com.github.cybellereaper.medusae.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.client.DiscordClientConfig;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordRestClientMessageRoutesTest {

    @Test
    void editMessageUsesPatchMessageRoute() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        DiscordRestClient restClient = buildRestClient(httpClient);

        restClient.editMessage("10", "20", Map.of("content", "updated"));

        assertEquals("PATCH", httpClient.lastRequest.method());
        assertEquals("/api/v10/channels/10/messages/20", httpClient.lastRequest.uri().getPath());
    }

    @Test
    void getChannelMessagesBuildsQueryString() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        DiscordRestClient restClient = buildRestClient(httpClient);

        restClient.getChannelMessages("10", Map.of("limit", 5, "before", "22"));

        assertEquals("GET", httpClient.lastRequest.method());
        assertEquals("/api/v10/channels/10/messages", httpClient.lastRequest.uri().getPath());
        Set<String> queryParams = new HashSet<>(Arrays.asList(httpClient.lastRequest.uri().getQuery().split("&")));
        assertEquals(Set.of("before=22", "limit=5"), queryParams);
    }

    @Test
    void addReactionUsesExpectedRoute() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        DiscordRestClient restClient = buildRestClient(httpClient);

        restClient.addReaction("10", "20", "🔥");

        assertEquals("PUT", httpClient.lastRequest.method());
        String rawPath = httpClient.lastRequest.uri().getRawPath();
        assertTrue(rawPath.startsWith("/api/v10/channels/10/messages/20/reactions/"));
        assertTrue(rawPath.endsWith("/@me"));
    }

    @Test
    void listPinnedMessagesUsesExpectedRoute() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        DiscordRestClient restClient = buildRestClient(httpClient);

        restClient.listPinnedMessages("10");

        assertEquals("GET", httpClient.lastRequest.method());
        assertEquals("/api/v10/channels/10/pins", httpClient.lastRequest.uri().getPath());
    }

    @Test
    void triggerTypingIndicatorUsesExpectedRoute() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        DiscordRestClient restClient = buildRestClient(httpClient);

        restClient.triggerTypingIndicator("10");

        assertEquals("POST", httpClient.lastRequest.method());
        assertEquals("/api/v10/channels/10/typing", httpClient.lastRequest.uri().getPath());
    }

    private static DiscordRestClient buildRestClient(HttpClient httpClient) {
        return new DiscordRestClient(
                httpClient,
                new ObjectMapper(),
                DiscordClientConfig.builder("token").build()
        );
    }

    private static final class CapturingHttpClient extends HttpClient {
        private HttpRequest lastRequest;

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            this.lastRequest = request;
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new FixedHttpResponse(request, "{}", 200);
            return response;
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            throw new UnsupportedOperationException("Not needed for tests");
        }

        @Override
        public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
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

    private record FixedHttpResponse(HttpRequest request, String body, int statusCode) implements HttpResponse<String> {
        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() {
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
}
