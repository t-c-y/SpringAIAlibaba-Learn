package com.example.springaialibaba.platform;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 极简 Request Guard：按 userId 做“每分钟请求上限”与调用计数。
 * 真实项目应换成 Sentinel / Resilience4j，并接鉴权中心 + 成本计费。
 */
@Component
public class RequestGuard {

    private static final int PER_MINUTE_LIMIT = 20;

    private final Map<String, Slot> slots = new ConcurrentHashMap<>();

    public boolean tryAcquire(String userId) {
        long now = System.currentTimeMillis() / 60000L;
        Slot s = slots.computeIfAbsent(userId, k -> new Slot(now));
        synchronized (s) {
            if (s.minute != now) { s.minute = now; s.count.set(0); }
            return s.count.incrementAndGet() <= PER_MINUTE_LIMIT;
        }
    }

    public int usage(String userId) {
        Slot s = slots.get(userId);
        return s == null ? 0 : s.count.get();
    }

    private static class Slot { long minute; final AtomicInteger count = new AtomicInteger(); Slot(long m) { minute = m; } }
}
