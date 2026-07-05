package com.printease.backend.controller;

import com.printease.backend.dto.request.ShopProfileUpdateRequest;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminProfileController {

    private final ShopService shopService;

    @GetMapping("/shops/{shopId}/profile")
    public ResponseEntity<ShopResponse> getProfile(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info(">>> GET /api/admin/shops/{}/profile | user={}", shopId, principal.getEmail());
        verifyShopOwnership(principal, shopId);
        ShopResponse response = shopService.getShopById(shopId);
        log.info("<<< GET /api/admin/shops/{}/profile | SUCCESS | shopName={}", shopId, response.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/shops/{shopId}/profile")
    public ResponseEntity<ShopResponse> updateProfile(
            @PathVariable UUID shopId,
            @RequestBody ShopProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info(">>> PUT /api/admin/shops/{}/profile | user={}", shopId, principal.getEmail());
        verifyShopOwnership(principal, shopId);
        ShopResponse response = shopService.updateShopProfile(shopId, request);
        log.info("<<< PUT /api/admin/shops/{}/profile | SUCCESS | shopName={}", shopId, response.getName());
        return ResponseEntity.ok(response);
    }

    private void verifyShopOwnership(UserPrincipal principal, UUID shopId) {
        if (principal.getShopId() == null || !principal.getShopId().equals(shopId)) {
            log.warn("ACCESS DENIED | user={} tried to access shopId={} but owns shopId={}",
                    principal.getEmail(), shopId, principal.getShopId());
            throw new AccessDeniedException("You do not have access to this shop");
        }
    }
}
