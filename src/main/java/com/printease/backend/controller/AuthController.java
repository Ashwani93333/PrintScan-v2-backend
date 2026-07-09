package com.printease.backend.controller;

import com.printease.backend.dto.request.ChangePasswordRequest;
import com.printease.backend.dto.request.LoginRequest;
import com.printease.backend.dto.response.JwtResponse;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.AuthService;
import com.printease.backend.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Value("${app.cookie.same-site}")
    private String cookieSameSite;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info(">>> POST /api/auth/login | email={}", request.getEmail());
        JwtResponse response = authService.login(request);
        
        ResponseCookie cookie = ResponseCookie.from("printease_token", response.getToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(jwtTokenProvider.getExpirationMs() / 1000)
                .build();

        log.info("<<< POST /api/auth/login | SUCCESS | user={} role={}", response.getEmail(), response.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info(">>> POST /api/auth/logout");
        ResponseCookie cookie = ResponseCookie.from("printease_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();

        log.info("<<< POST /api/auth/logout | SUCCESS | cookie cleared");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<JwtResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        log.info(">>> GET /api/auth/me | principal={}", principal != null ? principal.getEmail() : "null");
        if (principal == null) {
            log.warn("<<< GET /api/auth/me | 401 | No authenticated principal found");
            return ResponseEntity.status(401).build();
        }
        log.info("<<< GET /api/auth/me | SUCCESS | user={} role={}", principal.getEmail(), principal.getRole());
        return ResponseEntity.ok(JwtResponse.builder()
                .email(principal.getEmail())
                .role(principal.getRole())
                .name(principal.getName())
                .shopId(principal.getShopId() != null ? principal.getShopId().toString() : null)
                .build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info(">>> POST /api/auth/change-password | user={}", principal.getEmail());
        authService.changePassword(principal, request);
        log.info("<<< POST /api/auth/change-password | SUCCESS | user={}", principal.getEmail());
        return ResponseEntity.ok().build();
    }
}
