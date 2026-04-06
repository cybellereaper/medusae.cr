package com.github.cybellereaper.commands.core.execute;

public interface CommandExceptionHandler {
    void onException(CommandContext context, Throwable throwable);

    static CommandExceptionHandler rethrowing() {
        return (ctx, throwable) -> {
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(throwable);
        };
    }
}
