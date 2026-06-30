package com.api.bedhcd.shared.infrastructure.monitor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory store giữ tối đa MAX_SIZE request log gần nhất.
 * Thread-safe vì dùng ConcurrentLinkedDeque.
 */
@Component
public class RequestLogStore {

    private static final int MAX_SIZE = 500;

    private final Deque<RequestLogEntry> logs = new ConcurrentLinkedDeque<>();

    public void add(RequestLogEntry entry) {
        logs.addFirst(entry);
        while (logs.size() > MAX_SIZE) {
            logs.pollLast();
        }
    }

    /** Trả về toàn bộ log, mới nhất lên đầu */
    public List<RequestLogEntry> getAll() {
        return new ArrayList<>(logs);
    }

    /** Trả về N log gần nhất */
    public List<RequestLogEntry> getRecent(int limit) {
        return logs.stream().limit(limit).toList();
    }

    public long countErrors() {
        return logs.stream().filter(e -> e.getStatusCode() != null && e.getStatusCode() >= 400).count();
    }

    public long countTotal() {
        return logs.size();
    }
}
