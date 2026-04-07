package com.github.cybellereaper.medusae.commands.core.execute;

public interface CommandExceptionHandler {
    static CommandExceptionHandler rethrowing() {
        return (ctx, throwable) -> {
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(throwable);
        };
    }

    void onException(CommandContext context, Throwable throwable);
}
