package com.hashpass.security;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final long CLEANUP_INTERVAL = 1_000;
    private static final long STALE_WINDOW_EXTRA_MILLIS = 60L * 60L * 1000L;

    private final Map<String, WindowCounter> windows = new ConcurrentHashMap<>();
    private final AtomicLong accessCount = new AtomicLong();

    public boolean tryConsume(String bucketKey, int requests, int minutes) {
        long now = System.currentTimeMillis();
        WindowCounter counter = windows.compute(bucketKey, (key, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                return new WindowCounter(now, requests, minutes);
            }
            return existing;
        });

        boolean allowed;
        synchronized (counter) {
            if (counter.isExpired(now)) {
                counter.reset(now, requests, minutes);
            }
            counter.touch(now);
            allowed = counter.tryConsume();
        }

        if ((accessCount.incrementAndGet() % CLEANUP_INTERVAL) == 0) {
            cleanup(now);
        }

        return allowed;
    }

    private void cleanup(long now) {
        Iterator<Map.Entry<String, WindowCounter>> iterator = windows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WindowCounter> entry = iterator.next();
            WindowCounter counter = entry.getValue();
            if (counter != null && counter.isStale(now, STALE_WINDOW_EXTRA_MILLIS)) {
                windows.remove(entry.getKey(), counter);
            }
        }
    }

    private static final class WindowCounter {
        private long windowStart;
        private long windowEndsAt;
        private long lastAccessAt;
        private int remaining;

        private WindowCounter(long now, int requests, int minutes) {
            reset(now, requests, minutes);
        }

        private void reset(long now, int requests, int minutes) {
            this.windowStart = now;
            this.windowEndsAt = now + Math.max(1, minutes) * 60_000L;
            this.lastAccessAt = now;
            this.remaining = Math.max(1, requests);
        }

        private boolean isExpired(long now) {
            return now >= windowEndsAt;
        }

        private boolean isStale(long now, long extraMillis) {
            return now - lastAccessAt > (windowEndsAt - windowStart) + extraMillis;
        }

        private void touch(long now) {
            this.lastAccessAt = now;
        }

        private boolean tryConsume() {
            if (remaining <= 0) {
                return false;
            }
            remaining--;
            return true;
        }
    }
}