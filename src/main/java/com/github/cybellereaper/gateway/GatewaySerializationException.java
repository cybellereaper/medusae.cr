package com.github.cybellereaper.gateway;

/**
 * Raised when gateway payloads cannot be serialized or deserialized.
 */
public final class GatewaySerializationException extends GatewayClientException {
    public GatewaySerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
