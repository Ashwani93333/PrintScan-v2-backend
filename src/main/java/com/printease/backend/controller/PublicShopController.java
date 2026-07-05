package com.printease.backend.controller;

import com.printease.backend.dto.request.RegisterShopRequest;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/shops")
@RequiredArgsConstructor
@Slf4j
public class PublicShopController {

    private final ShopService shopService;

    @GetMapping("/{slug}")
    public ResponseEntity<ShopResponse> getShopBySlug(@PathVariable String slug) {
        log.info(">>> GET /api/public/shops/{}", slug);
        ShopResponse response = shopService.getPublicShopBySlug(slug);
        log.info("<<< GET /api/public/shops/{} | SUCCESS | shopName={}", slug, response.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{slug}/qr-visit")
    public ResponseEntity<Void> incrementQrVisit(@PathVariable String slug) {
        log.info(">>> POST /api/public/shops/{}/qr-visit", slug);
        shopService.incrementQrVisits(slug);
        log.info("<<< POST /api/public/shops/{}/qr-visit | SUCCESS", slug);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<ShopResponse> registerShop(@Valid @RequestBody RegisterShopRequest request) {
        log.info(">>> POST /api/public/shops/register | shopName={} | slug={} | adminEmail={}",
                request.getName(), request.getSlug(), request.getAdminEmail());
        ShopResponse response = shopService.registerShop(request);
        log.info("<<< POST /api/public/shops/register | CREATED | shopId={} | slug={}", response.getId(), response.getSlug());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
