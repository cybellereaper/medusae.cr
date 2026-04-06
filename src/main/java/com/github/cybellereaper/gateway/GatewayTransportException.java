package com.github.cybellereaper.gateway;

/**
 * Raised when network transport operations fail.
 */
public final class GatewayTransportException extends GatewayClientException {
    public GatewayTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
