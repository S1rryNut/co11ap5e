package com.personal.site.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private final Map<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public synchronized boolean allow(String address) {
        Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
        ArrayDeque<Instant> values = attempts.computeIfAbsent(address, ignored -> new ArrayDeque<>());
        while (!values.isEmpty() && values.peekFirst().isBefore(cutoff)) values.removeFirst();
        if (values.size() >= MAX_ATTEMPTS) return false;
        values.addLast(Instant.now());
        return true;
    }

    public synchronized void clear(String address) {
        attempts.remove(address);
    }
}

