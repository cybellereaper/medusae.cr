package com.github.cybellereaper.client;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class BatchOperationExecutor {
    <T> void executeAll(List<T> operations, Consumer<T> operationExecutor) {
        Objects.requireNonNull(operations, "operations");
        Objects.requireNonNull(operationExecutor, "operationExecutor");

        RuntimeException firstFailure = null;
        for (T operation : operations) {
            try {
                operationExecutor.accept(operation);
            } catch (RuntimeException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                } else {
                    firstFailure.addSuppressed(failure);
                }
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
    }
}
