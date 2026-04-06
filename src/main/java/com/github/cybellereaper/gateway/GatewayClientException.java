package com.github.cybellereaper.gateway;

/**
 * Base type for recoverable and non-recoverable gateway failures.
 */
public sealed class GatewayClientException extends RuntimeException
        permits GatewayProtocolException, GatewaySerializationException, GatewayTransportException {
    public GatewayClientException(String message) {
        super(message);
    }

    public GatewayClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
