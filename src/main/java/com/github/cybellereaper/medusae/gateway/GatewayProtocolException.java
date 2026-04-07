package com.github.cybellereaper.medusae.gateway;

/**
 * Raised when Discord gateway protocol payloads violate required invariants.
 */
public final class GatewayProtocolException extends GatewayClientException {
    public GatewayProtocolException(String message) {
        super(message);
    }

    public GatewayProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
