package com.example.googlelogin.controller;

import com.example.googlelogin.model.AuthRequest;
import com.example.googlelogin.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestBody AuthRequest req) {
        if (req.email() == null || !req.email().contains("@")) {
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
        if (!authService.checkPassword(req.email(), req.password())) {
            return ResponseEntity.status(401).body(Map.of("error", "密碼錯誤，請再試一次"));
        }
        ResponseCookie cookie = ResponseCookie.from("auth_email", req.email())
                .httpOnly(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("Lax")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("ok", true));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        ResponseCookie cookie = ResponseCookie.from("auth_email", "")
                .httpOnly(true)
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
        String email = Arrays.stream(request.getCookies() != null ? request.getCookies() : new jakarta.servlet.http.Cookie[0])
                .filter(c -> "auth_email".equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (email == null || email.isBlank() || !authService.userExists(email)) {
            return ResponseEntity.status(401).body(Map.of("error", "未登入"));
        }
        return ResponseEntity.ok(Map.of("email", email));
    }
}
