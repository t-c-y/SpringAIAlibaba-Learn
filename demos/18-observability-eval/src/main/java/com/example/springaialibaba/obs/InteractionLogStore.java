package com.example.springaialibaba.obs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * 简易日志存储（教学）。生产上应写入 ELK / Loki / ClickHouse，并接入 Traces / Metrics。
 */
@Component
public class InteractionLogStore {

    private static final int MAX_SIZE = 500;
    private final ConcurrentLinkedDeque<Entry> buffer = new ConcurrentLinkedDeque<>();

    public String log(Entry e) {
        if (buffer.size() >= MAX_SIZE) buffer.pollFirst();
        buffer.addLast(e);
        return e.id;
    }

    public List<Entry> tail(int n) {
        List<Entry> list = new ArrayList<>(buffer);
        int from = Math.max(0, list.size() - n);
        return list.subList(from, list.size());
    }

    public static Entry newEntry(String userId, String question, String answer,
                                 long latencyMs, Integer estimatedTokens) {
        return new Entry(UUID.randomUUID().toString(), Instant.now().toString(),
                userId, question, answer, latencyMs, estimatedTokens);
    }

    public record Entry(String id, String timestamp, String userId, String question,
                        String answer, long latencyMs, Integer estimatedTokens) {}
}
