package com.thinkai.backend.ai.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CacheMetrics {

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong saveCount = new AtomicLong(0);
    private final AtomicLong invalidateCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    public void recordHit() {
        hitCount.incrementAndGet();
    }

    public void recordMiss() {
        missCount.incrementAndGet();
    }

    public void recordSave() {
        saveCount.incrementAndGet();
    }

    public void recordInvalidate() {
        invalidateCount.incrementAndGet();
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        return total > 0 ? (double) hitCount.get() / total : 0.0;
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public long getSaveCount() {
        return saveCount.get();
    }

    public long getInvalidateCount() {
        return invalidateCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }
}
