package com.github.cybellereaper.http;

public final class DiscordHttpException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public DiscordHttpException(int statusCode, String responseBody) {
        super("Discord API request failed with status " + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    public String responseBody() {
        return responseBody;
    }
}
