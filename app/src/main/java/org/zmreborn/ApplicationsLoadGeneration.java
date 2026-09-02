package org.zmreborn;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe generation counter for tracking and discarding stale application loading batches.
 */
final class ApplicationsLoadGeneration {
    private final AtomicInteger current = new AtomicInteger();

    /**
     * Increments and returns a new generation identifier for a load task.
     */
    int start() {
        return this.current.incrementAndGet();
    }

    /**
     * Checks if the given generation matches the most recently started generation.
     */
    boolean isCurrent(int generation) {
        return this.current.get() == generation;
    }
}
