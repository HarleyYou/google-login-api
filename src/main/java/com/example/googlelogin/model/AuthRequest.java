package com.example.googlelogin.model;

public record AuthRequest(String email, String password) {
    @Override
    public String toString() {
        return "AuthRequest[email=" + email + ", password=***]";
    }
}
