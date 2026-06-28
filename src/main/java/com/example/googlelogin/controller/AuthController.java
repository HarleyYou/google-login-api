package com.example.googlelogin.controller;

import com.example.googlelogin.model.AuthRequest;
import com.example.googlelogin.service.AuthService;
import com.example.googlelogin.service.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final SessionStore sessionStore;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

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
            log.warn("check-email: 帳號不存在 [{}]", req.email());
            return ResponseEntity.status(404).body(Map.of("error", "找不到這個 Google 帳戶"));
        }
        log.info("check-email: 帳號存在 [{}]", req.email());
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
            log.warn("login: 驗證失敗 [{}]", req.email());
            return ResponseEntity.status(401).body(Map.of("error", "帳號或密碼錯誤"));
        }
        String sessionId = sessionStore.create(req.email());
        log.info("login: 登入成功 [{}]", req.email());
        ResponseCookie cookie = buildCookie("session_id", sessionId, 60 * 60 * 24);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("ok", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        sessionStore.remove(sessionId);
        log.info("logout: session 已銷毀");
        ResponseCookie cookie = buildCookie("session_id", "", 0);
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

    private ResponseCookie buildCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
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
