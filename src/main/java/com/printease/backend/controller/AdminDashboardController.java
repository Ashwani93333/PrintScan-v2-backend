package com.printease.backend.controller;

import com.printease.backend.dto.response.DashboardStatsResponse;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.PrintJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final PrintJobService printJobService;

    @GetMapping("/shops/{shopId}/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboard(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal UserPrincipal principal) {
        verifyShopOwnership(principal, shopId);
        return ResponseEntity.ok(printJobService.getDashboardStats(shopId));
    }

    private void verifyShopOwnership(UserPrincipal principal, UUID shopId) {
        if (principal.getShopId() == null || !principal.getShopId().equals(shopId)) {
            throw new AccessDeniedException("You do not have access to this shop");
        }
    }
}
