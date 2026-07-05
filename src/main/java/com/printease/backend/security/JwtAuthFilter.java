package com.printease.backend.security;

import com.printease.backend.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CachedUserService cachedUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        try {
            String jwt = extractJwt(request);
            if (jwt != null && tokenProvider.validateToken(jwt)) {
                UUID userId = tokenProvider.getUserIdFromToken(jwt);
                User user = cachedUserService.getUserById(userId).orElse(null);

                if (user != null && user.getIsActive()) {
                    // Resolve shopId for ADMIN users
                    UUID shopId = null;
                    String shopIdClaim = tokenProvider.getShopIdFromToken(jwt);
                    if (shopIdClaim != null) {
                        shopId = UUID.fromString(shopIdClaim);
                    } else {
                        // Fallback: look up shop by admin_id
                        shopId = cachedUserService.getShopIdByAdminId(user.getId()).orElse(null);
                    }

                    UserPrincipal principal = UserPrincipal.create(user, shopId);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("AUTH OK | {} {} | user={} | role={}", method, uri, user.getEmail(), user.getRole());
                } else {
                    log.warn("AUTH FAIL | {} {} | userId={} | user={} active={}",
                            method, uri, userId,
                            user != null ? "found" : "NOT_FOUND",
                            user != null ? user.getIsActive() : "N/A");
                }
            } else if (jwt != null) {
                log.warn("AUTH FAIL | {} {} | invalid/expired JWT token", method, uri);
            }
        } catch (Exception ex) {
            log.error("AUTH ERROR | {} {} | Could not set user authentication: {}", method, uri, ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwt(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("printease_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
