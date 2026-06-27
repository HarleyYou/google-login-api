package com.example.googlelogin.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionStore {

    private final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String create(String email) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(sessionId, email);
        return sessionId;
    }

    public String getEmail(String sessionId) {
        return sessionId == null ? null : sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }
}
