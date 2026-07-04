package com.printease.backend.controller;

import com.printease.backend.dto.request.ShopCreateRequest;
import com.printease.backend.dto.response.AnalyticsResponse;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.service.AnalyticsService;
import com.printease.backend.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class SuperAdminController {

    private final ShopService shopService;
    private final AnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        // Default range: last 30 days to now
        Instant fromInstant = from != null
                ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant toInstant = to != null
                ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();

        return ResponseEntity.ok(analyticsService.getAnalytics(fromInstant, toInstant));
    }

    @GetMapping("/shops/{shopId}/analytics")
    public ResponseEntity<com.printease.backend.dto.response.ShopFileStatsResponse> getShopFileStats(
            @PathVariable UUID shopId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
            
        Instant fromInstant = from != null
                ? LocalDate.parse(from).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now().minusSeconds(30L * 24 * 60 * 60);
        Instant toInstant = to != null
                ? LocalDate.parse(to).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.now();
                
        return ResponseEntity.ok(analyticsService.getShopFileStats(shopId, fromInstant, toInstant));
    }

    @GetMapping("/shops")
    public ResponseEntity<Page<ShopResponse>> getAllShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(shopService.getAllShops(pageable));
    }

    @PostMapping("/shops")
    public ResponseEntity<ShopResponse> createShop(@Valid @RequestBody ShopCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shopService.createShopByAdmin(request));
    }

    @PostMapping("/shops/{shopId}/approve")
    public ResponseEntity<ShopResponse> approveShop(@PathVariable UUID shopId) {
        return ResponseEntity.ok(shopService.approveShop(shopId));
    }

    @PostMapping("/shops/{shopId}/reject")
    public ResponseEntity<Void> rejectShop(@PathVariable UUID shopId) {
        shopService.rejectShop(shopId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/shops/{shopId}/toggle-active")
    public ResponseEntity<ShopResponse> toggleActive(@PathVariable UUID shopId) {
        return ResponseEntity.ok(shopService.toggleActive(shopId));
    }

    @DeleteMapping("/shops/{shopId}")
    public ResponseEntity<Void> deleteShop(@PathVariable UUID shopId) {
        shopService.deleteShop(shopId);
        return ResponseEntity.noContent().build();
    }
}
