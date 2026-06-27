package com.example.googlelogin.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
public class AuthService {

    // 密碼以 SHA-256 hash 儲存
    // "password123" -> sha256, "test1234" -> sha256
    private static final Map<String, String> USERS = Map.of(
        "user@gmail.com", sha256("password123"),
        "test@gmail.com", sha256("test1234")
    );

    public boolean userExists(String email) {
        return USERS.containsKey(email);
    }

    public boolean checkPassword(String email, String password) {
        String stored = USERS.get(email);
        return stored != null && stored.equals(sha256(password));
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
