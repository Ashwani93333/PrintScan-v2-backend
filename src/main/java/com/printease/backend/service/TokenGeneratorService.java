package com.printease.backend.service;

import com.printease.backend.repository.PrintJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenGeneratorService {

    private final PrintJobRepository printJobRepository;

    public String generateTokenForShop(UUID shopId) {
        long count = printJobRepository.countByShopId(shopId);
        return String.valueOf(count + 1);
    }
}
