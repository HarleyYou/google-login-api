package com.example.googlelogin.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class SessionStore {

    private static final long SESSION_TTL_SECONDS = 60 * 60 * 24;

    private record Entry(String email, long expiresAt) {}

    private final ConcurrentHashMap<String, Entry> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public SessionStore() {
        // purge expired sessions every hour
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleanup");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::purgeExpired, 1, 1, TimeUnit.HOURS);
    }

    public String create(String email) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expiresAt = System.currentTimeMillis() + SESSION_TTL_SECONDS * 1000;
        sessions.put(sessionId, new Entry(email, expiresAt));
        return sessionId;
    }

    public String getEmail(String sessionId) {
        if (sessionId == null) return null;
        Entry entry = sessions.get(sessionId);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expiresAt()) {
            sessions.remove(sessionId);
            return null;
        }
        return entry.email();
    }

    public void remove(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> now > e.getValue().expiresAt());
    }
}
