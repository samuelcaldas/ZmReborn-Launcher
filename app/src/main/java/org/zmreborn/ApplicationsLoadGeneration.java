package org.zmreborn;

import java.util.concurrent.atomic.AtomicInteger;

final class ApplicationsLoadGeneration {
    private final AtomicInteger mCurrent = new AtomicInteger();

    int start() {
        return this.mCurrent.incrementAndGet();
    }

    boolean isCurrent(int generation) {
        return this.mCurrent.get() == generation;
    }
}
