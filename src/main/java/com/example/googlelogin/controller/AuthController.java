package com.example.googlelogin.controller;

import com.example.googlelogin.model.AuthRequest;
import com.example.googlelogin.service.AuthService;
import com.example.googlelogin.service.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionStore sessionStore;

    public AuthController(AuthService authService, SessionStore sessionStore) {
        this.authService = authService;
        this.sessionStore = sessionStore;
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody AuthRequest req) {
        if (req.email() == null || req.email().length() > 254 || !req.email().matches("^[^@]+@[^@]+\\.[^@]+$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "請輸入有效的電子郵件地址"));
        }
        if (!authService.userExists(req.email())) {
            return ResponseEntity.status(404).body(Map.of("error", "找不到這個 Google 帳戶"));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest req) {
        if (req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "請輸入電子郵件與密碼"));
        }
        if (req.password().length() > 128) {
            return ResponseEntity.badRequest().body(Map.of("error", "密碼格式不正確"));
        }
        if (!authService.checkPassword(req.email(), req.password())) {
            return ResponseEntity.status(401).body(Map.of("error", "帳號或密碼錯誤"));
        }
        String sessionId = sessionStore.create(req.email());
        ResponseCookie cookie = ResponseCookie.from("session_id", sessionId)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("ok", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        sessionStore.remove(sessionId);
        ResponseCookie cookie = ResponseCookie.from("session_id", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        String email = sessionStore.getEmail(sessionId);
        if (email == null || !authService.userExists(email)) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        return ResponseEntity.ok(Map.of("email", email));
    }

    private String getSessionId(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "session_id".equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
