package com.printease.backend.controller;

import com.printease.backend.dto.request.ChangePasswordRequest;
import com.printease.backend.dto.request.LoginRequest;
import com.printease.backend.dto.response.JwtResponse;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.AuthService;
import com.printease.backend.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CsrfTokenRepository csrfTokenRepository;

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

    @GetMapping("/csrf")
    public ResponseEntity<Void> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        log.info(">>> GET /api/auth/csrf");
        // In Spring Security 6, CookieCsrfTokenRepository is lazy — the XSRF-TOKEN cookie
        // is only written when the token is explicitly loaded via the repository.
        // Using request.getAttribute gives a DeferredCsrfToken (Supplier), not the raw token,
        // so we use the repository directly to force the cookie to be written.
        CsrfToken csrf = csrfTokenRepository.loadToken(request);
        if (csrf == null) {
            csrf = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(csrf, request, response);
            log.info("<<< GET /api/auth/csrf | SUCCESS | new CSRF token generated");
        } else {
            // Re-save to ensure cookie is present on this response
            csrfTokenRepository.saveToken(csrf, request, response);
            log.info("<<< GET /api/auth/csrf | SUCCESS | existing CSRF token re-saved");
        }
        return ResponseEntity.ok().build();
    }
}
