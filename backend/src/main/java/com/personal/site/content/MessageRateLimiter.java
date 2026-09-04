package com.personal.site.content;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageRateLimiter {
    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_SECONDS = 600;
    private static final int MAX_INTERACTIONS = 15;
    private static final long INTERACTION_WINDOW_SECONDS = 600;
    private final Map<String, ArrayDeque<Instant>> requests = new ConcurrentHashMap<>();
    private final Map<String, ArrayDeque<Instant>> interactions = new ConcurrentHashMap<>();

    public void check(String address) {
        Instant now = Instant.now();
        ArrayDeque<Instant> history = requests.computeIfAbsent(address == null ? "unknown" : address, ignored -> new ArrayDeque<>());
        synchronized (history) {
            while (!history.isEmpty() && history.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) history.removeFirst();
            if (history.size() >= MAX_REQUESTS) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "留言提交过于频繁，请稍后再试");
            history.addLast(now);
        }
    }

    public void checkInteraction(String address) {
        Instant now = Instant.now();
        ArrayDeque<Instant> history = interactions.computeIfAbsent(address == null ? "unknown" : address, ignored -> new ArrayDeque<>());
        synchronized (history) {
            while (!history.isEmpty() && history.peekFirst().isBefore(now.minusSeconds(INTERACTION_WINDOW_SECONDS))) history.removeFirst();
            if (history.size() >= MAX_INTERACTIONS) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "操作过于频繁，请稍后再试");
            history.addLast(now);
        }
    }
}
