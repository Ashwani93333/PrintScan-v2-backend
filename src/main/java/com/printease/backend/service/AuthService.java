package com.printease.backend.service;

import com.printease.backend.dto.request.ChangePasswordRequest;
import com.printease.backend.dto.request.LoginRequest;
import com.printease.backend.dto.response.JwtResponse;
import com.printease.backend.entity.Shop;
import com.printease.backend.entity.User;
import com.printease.backend.exception.BadRequestException;
import com.printease.backend.exception.UnauthorizedException;
import com.printease.backend.repository.ShopRepository;
import com.printease.backend.repository.UserRepository;
import com.printease.backend.security.JwtTokenProvider;
import com.printease.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public JwtResponse login(LoginRequest request) {
        log.info("Login attempt for email={}", request.getEmail());
        
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login FAILED | email={} | user not found", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!user.getIsActive()) {
            log.warn("Login FAILED | email={} | account disabled", request.getEmail());
            throw new UnauthorizedException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login FAILED | email={} | wrong password", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        // Resolve shopId and shopSlug for ADMIN users
        UUID shopId = null;
        String shopSlug = null;
        if (user.getRole().name().equals("ADMIN")) {
            Shop shop = shopRepository.findByAdminId(user.getId()).orElse(null);
            if (shop != null) {
                shopId = shop.getId();
                shopSlug = shop.getSlug();
                log.info("Login | Resolved shop for ADMIN | shopId={} | slug={}", shopId, shopSlug);
            } else {
                log.warn("Login | ADMIN user has no shop assigned | userId={}", user.getId());
            }
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name(), shopId);

        log.info("Login SUCCESS | email={} | role={} | shopId={}", user.getEmail(), user.getRole(), shopId);
        return JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .role(user.getRole().name())
                .name(user.getName())
                .email(user.getEmail())
                .shopId(shopId != null ? shopId.toString() : null)
                .shopSlug(shopSlug)
                .build();
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        log.info("Change password attempt | userId={} | email={}", principal.getId(), principal.getEmail());
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> {
                    log.error("Change password FAILED | userId={} | user not found in DB", principal.getId());
                    return new UnauthorizedException("User not found");
                });

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Change password FAILED | email={} | current password incorrect", user.getEmail());
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }
}
