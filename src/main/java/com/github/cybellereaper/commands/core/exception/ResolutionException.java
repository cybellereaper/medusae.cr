package com.github.cybellereaper.commands.core.exception;

public final class ResolutionException extends CommandFrameworkException {
    public ResolutionException(String message) {
        super(message);
    }

    public ResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
