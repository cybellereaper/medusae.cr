package com.github.cybellereaper.medusae.commands.core.exception;

public class CommandFrameworkException extends RuntimeException {
    public CommandFrameworkException(String message) {
        super(message);
    }

    public CommandFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
