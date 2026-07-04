package com.printease.backend.controller;

import com.printease.backend.dto.request.ShopProfileUpdateRequest;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProfileController {

    private final ShopService shopService;

    @GetMapping("/shops/{shopId}/profile")
    public ResponseEntity<ShopResponse> getProfile(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal UserPrincipal principal) {
        verifyShopOwnership(principal, shopId);
        return ResponseEntity.ok(shopService.getShopById(shopId));
    }

    @PutMapping("/shops/{shopId}/profile")
    public ResponseEntity<ShopResponse> updateProfile(
            @PathVariable UUID shopId,
            @RequestBody ShopProfileUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        verifyShopOwnership(principal, shopId);
        return ResponseEntity.ok(shopService.updateShopProfile(shopId, request));
    }

    private void verifyShopOwnership(UserPrincipal principal, UUID shopId) {
        if (principal.getShopId() == null || !principal.getShopId().equals(shopId)) {
            throw new AccessDeniedException("You do not have access to this shop");
        }
    }
}
