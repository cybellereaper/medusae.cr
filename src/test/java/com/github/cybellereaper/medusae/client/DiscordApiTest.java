package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.cybellereaper.medusae.http.DiscordRestClient;
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
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DiscordApiTest {

    @Test
    void getChannelUsesExpectedRoute() {
        CapturingHttpClient httpClient = new CapturingHttpClient("{\"id\":\"123\"}", 200);
        DiscordApi api = buildApi(httpClient);

        JsonNode response = api.getChannel("123");

        assertEquals("/api/v10/channels/123", httpClient.lastRequest.uri().getPath());
        assertEquals("123", response.path("id").asText());
    }

    @Test
    void requestRequiresLeadingSlashInPath() {
        DiscordApi api = buildApi(new CapturingHttpClient("{}", 200));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> api.request("GET", "users/@me", null)
        );

        assertEquals("path must start with '/'", exception.getMessage());
    }

    @Test
    void deleteMessageRejectsBlankMessageId() {
        DiscordApi api = buildApi(new CapturingHttpClient("{}", 200));

        assertThrows(IllegalArgumentException.class, () -> api.deleteMessage("123", " "));
    }

    private static DiscordApi buildApi(HttpClient httpClient) {
        DiscordRestClient restClient = new DiscordRestClient(
                httpClient,
                new ObjectMapper(),
                DiscordClientConfig.builder("token").build()
        );
        return new DiscordApi(restClient);
    }

    private static final class CapturingHttpClient extends HttpClient {
        private final String responseBody;
        private final int responseStatus;
        private HttpRequest lastRequest;

        private CapturingHttpClient(String responseBody, int responseStatus) {
            this.responseBody = responseBody;
            this.responseStatus = responseStatus;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            this.lastRequest = request;
            @SuppressWarnings("unchecked")
            HttpResponse<T> response = (HttpResponse<T>) new FixedHttpResponse(request, responseBody, responseStatus);
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
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
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
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
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
