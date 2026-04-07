package com.github.cybellereaper.medusae.client;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

public record DiscordClientConfig(
        String botToken,
        int intents,
        String apiBaseUrl,
        int apiVersion,
        Duration requestTimeout,
        int shardId,
        int shardCount
) {
    public DiscordClientConfig {
        Objects.requireNonNull(botToken, "botToken");
        if (botToken.isBlank()) {
            throw new IllegalArgumentException("botToken must not be blank");
        }

        apiBaseUrl = (apiBaseUrl == null || apiBaseUrl.isBlank())
                ? "https://discord.com/api"
                : apiBaseUrl.replaceAll("/+$", "");

        apiVersion = apiVersion <= 0 ? 10 : apiVersion;
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(30) : requestTimeout;

        if (shardCount < 1) {
            throw new IllegalArgumentException("shardCount must be >= 1");
        }
        if (shardId < 0 || shardId >= shardCount) {
            throw new IllegalArgumentException("shardId must be between 0 (inclusive) and shardCount (exclusive)");
        }
    }

    public static Builder builder(String botToken) {
        return new Builder(botToken);
    }

    public URI apiUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(apiBaseUrl + "/v" + apiVersion + normalizedPath);
    }

    public static final class Builder {
        private final String botToken;
        private int intents;
        private String apiBaseUrl = "https://discord.com/api";
        private int apiVersion = 10;
        private Duration requestTimeout = Duration.ofSeconds(30);
        private int shardId = 0;
        private int shardCount = 1;

        private Builder(String botToken) {
            this.botToken = botToken;
        }

        public Builder intents(int intents) {
            this.intents = intents;
            return this;
        }

        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        public Builder apiVersion(int apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder shard(int shardId, int shardCount) {
            this.shardId = shardId;
            this.shardCount = shardCount;
            return this;
        }

        public DiscordClientConfig build() {
            return new DiscordClientConfig(botToken, intents, apiBaseUrl, apiVersion, requestTimeout, shardId, shardCount);
        }
    }
}
