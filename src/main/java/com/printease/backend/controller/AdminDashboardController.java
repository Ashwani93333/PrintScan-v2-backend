package com.printease.backend.controller;

import com.printease.backend.dto.response.DashboardStatsResponse;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.PrintJobService;
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
public class AdminDashboardController {

    private final PrintJobService printJobService;

    @GetMapping("/shops/{shopId}/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboard(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info(">>> GET /api/admin/shops/{}/dashboard | user={}", shopId, principal.getEmail());
        verifyShopOwnership(principal, shopId);
        DashboardStatsResponse stats = printJobService.getDashboardStats(shopId);
        log.info("<<< GET /api/admin/shops/{}/dashboard | SUCCESS | totalJobs={}", shopId, stats.getTotalJobs());
        return ResponseEntity.ok(stats);
    }

    private void verifyShopOwnership(UserPrincipal principal, UUID shopId) {
        if (principal.getShopId() == null || !principal.getShopId().equals(shopId)) {
            log.warn("ACCESS DENIED | user={} tried to access shopId={} but owns shopId={}",
                    principal.getEmail(), shopId, principal.getShopId());
            throw new AccessDeniedException("You do not have access to this shop");
        }
    }
}
