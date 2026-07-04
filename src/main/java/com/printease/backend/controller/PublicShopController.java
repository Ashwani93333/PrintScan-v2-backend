package com.printease.backend.controller;

import com.printease.backend.dto.request.RegisterShopRequest;
import com.printease.backend.dto.response.ShopResponse;
import com.printease.backend.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/shops")
@RequiredArgsConstructor
public class PublicShopController {

    private final ShopService shopService;

    @GetMapping("/{slug}")
    public ResponseEntity<ShopResponse> getShopBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(shopService.getPublicShopBySlug(slug));
    }

    @PostMapping("/{slug}/qr-visit")
    public ResponseEntity<Void> incrementQrVisit(@PathVariable String slug) {
        shopService.incrementQrVisits(slug);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<ShopResponse> registerShop(@Valid @RequestBody RegisterShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shopService.registerShop(request));
    }
}
