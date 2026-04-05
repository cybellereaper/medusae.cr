package com.github.cybellereaper.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchOperationExecutorTest {

    @Test
    void executesAllOperationsEvenWhenSomeFail() {
        BatchOperationExecutor executor = new BatchOperationExecutor();
        AtomicInteger processed = new AtomicInteger(0);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                executor.executeAll(List.of("first", "second", "third"), value -> {
                    processed.incrementAndGet();
                    if ("first".equals(value) || "second".equals(value)) {
                        throw new RuntimeException("failed: " + value);
                    }
                })
        );

        assertEquals(3, processed.get());
        assertEquals("failed: first", error.getMessage());
        assertEquals(1, error.getSuppressed().length);
        assertEquals("failed: second", error.getSuppressed()[0].getMessage());
    }

    @Test
    void doesNotThrowWhenAllOperationsSucceed() {
        BatchOperationExecutor executor = new BatchOperationExecutor();
        AtomicInteger processed = new AtomicInteger(0);

        executor.executeAll(List.of(1, 2, 3), ignored -> processed.incrementAndGet());

        assertEquals(3, processed.get());
    }
}
