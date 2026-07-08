package com.printease.backend.controller;

import com.printease.backend.dto.request.ShopCreateRequest;
import com.printease.backend.dto.response.AnalyticsResponse;
import com.printease.backend.dto.response.ShopFileStatsResponse;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.service.AnalyticsService;
import com.printease.backend.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/api/super")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminController {

    private final ShopService shopService;
    private final AnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        log.info(">>> GET /api/super/analytics | from={} | to={}", from, to);

        // Default range: last 30 days to now
        Instant fromInstant = from != null
                ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant toInstant = to != null
                ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        AnalyticsResponse response = analyticsService.getAnalytics(fromInstant, toInstant);
        log.info("<<< GET /api/super/analytics | SUCCESS | totalShops={} | totalJobs={}",
                response.getTotalShops(), response.getTotalJobs());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shops/{shopId}/analytics")
    public ResponseEntity<ShopFileStatsResponse> getShopFileStats(
            @PathVariable UUID shopId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        log.info(">>> GET /api/super/shops/{}/analytics | from={} | to={}", shopId, from, to);
            
        Instant fromInstant = from != null
                ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant toInstant = to != null
                ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();
                
        var response = analyticsService.getShopFileStats(shopId, fromInstant, toInstant);
        log.info("<<< GET /api/super/shops/{}/analytics | SUCCESS | totalJobs={} | completedJobs={}",
                shopId, response.getTotalJobs(), response.getCompletedJobs());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shops")
    public ResponseEntity<Page<ShopResponse>> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info(">>> GET /api/super/shops | page={} | size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ShopResponse> result = shopService.getAllShops(pageable);
        log.info("<<< GET /api/super/shops | SUCCESS | totalElements={} | totalPages={}",
                result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shops/{shopId}")
    public ResponseEntity<ShopResponse> getShopDetails(@PathVariable UUID shopId) {
        log.info(">>> GET /api/super/shops/{}", shopId);
        ShopResponse response = shopService.getShopById(shopId);
        log.info("<<< GET /api/super/shops/{} | SUCCESS | name={} | approved={}", shopId, response.getName(), response.getIsApproved());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shops")
    public ResponseEntity<ShopResponse> createShop(@Valid @RequestBody ShopCreateRequest request) {
        log.info(">>> POST /api/super/shops | shopName={} | slug={}", request.getName(), request.getSlug());
        ShopResponse response = shopService.createShopByAdmin(request);
        log.info("<<< POST /api/super/shops | CREATED | shopId={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shops/{shopId}/approve")
    public ResponseEntity<ShopResponse> approveShop(@PathVariable UUID shopId) {
        log.info(">>> POST /api/super/shops/{}/approve", shopId);
        ShopResponse response = shopService.approveShop(shopId);
        log.info("<<< POST /api/super/shops/{}/approve | SUCCESS | admin user activated", shopId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shops/{shopId}/disapprove")
    public ResponseEntity<ShopResponse> disapproveShop(@PathVariable UUID shopId) {
        log.info(">>> POST /api/super/shops/{}/disapprove", shopId);
        ShopResponse response = shopService.disapproveShop(shopId);
        log.info("<<< POST /api/super/shops/{}/disapprove | SUCCESS | admin user deactivated", shopId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shops/{shopId}/reject")
    public ResponseEntity<Void> rejectShop(@PathVariable UUID shopId) {
        log.info(">>> POST /api/super/shops/{}/reject", shopId);
        shopService.rejectShop(shopId);
        log.info("<<< POST /api/super/shops/{}/reject | SUCCESS | shop + admin user deleted", shopId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/shops/{shopId}")
    public ResponseEntity<Void> deleteShop(@PathVariable UUID shopId) {
        log.info(">>> DELETE /api/super/shops/{}", shopId);
        shopService.deleteShop(shopId);
        log.info("<<< DELETE /api/super/shops/{} | SUCCESS | shop + admin user deleted", shopId);
        return ResponseEntity.noContent().build();
    }
}
