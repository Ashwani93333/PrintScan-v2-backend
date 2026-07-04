package com.printease.backend.service;

import com.printease.backend.dto.response.AnalyticsResponse;
import com.printease.backend.entity.PrintJob;
import com.printease.backend.entity.Shop;
import com.printease.backend.entity.enums.JobStatus;
import com.printease.backend.repository.PrintJobRepository;
import com.printease.backend.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ShopRepository shopRepository;
    private final PrintJobRepository printJobRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Instant from, Instant to) {
        long totalShops = shopRepository.count();
        long activeShops = shopRepository.countByIsActiveTrue();
        long totalJobs = printJobRepository.countByCreatedAtBetween(from, to);
        long completedJobs = printJobRepository.countByStatus(JobStatus.COMPLETED);
        long cancelledJobs = printJobRepository.countByStatus(JobStatus.CANCELLED);

        List<PrintJob> jobsInRange = printJobRepository.findAllByCreatedAtBetween(from, to);

        // Jobs by date
        Map<LocalDate, Long> byDate = jobsInRange.stream()
                .collect(Collectors.groupingBy(
                        j -> j.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.counting()));

        List<Map<String, Object>> jobsByDate = byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey().toString());
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());

        // Shop completion rates
        List<Shop> allShops = shopRepository.findAll();
        List<Map<String, Object>> shopRates = allShops.stream().map(shop -> {
            long shopTotal = printJobRepository.countByShopId(shop.getId());
            long shopCompleted = printJobRepository.countByShopIdAndStatus(shop.getId(), JobStatus.COMPLETED);
            double rate = shopTotal > 0 ? (double) shopCompleted / shopTotal * 100 : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shopId", shop.getId().toString());
            m.put("shopName", shop.getName());
            m.put("totalJobs", shopTotal);
            m.put("completedJobs", shopCompleted);
            m.put("completionRate", Math.round(rate * 100.0) / 100.0);
            return m;
        }).collect(Collectors.toList());

        return AnalyticsResponse.builder()
                .totalShops(totalShops)
                .activeShops(activeShops)
                .totalJobs(totalJobs)
                .completedJobs(completedJobs)
                .cancelledJobs(cancelledJobs)
                .jobsByDate(jobsByDate)
                .shopCompletionRates(shopRates)
                .build();
    }

    @Transactional(readOnly = true)
    public com.printease.backend.dto.response.ShopFileStatsResponse getShopFileStats(UUID shopId, Instant from, Instant to) {
        long totalJobs = printJobRepository.countByShopIdAndCreatedAtBetween(shopId, from, to);
        long completedJobs = printJobRepository.countByShopIdAndStatusAndCreatedAtBetween(shopId, JobStatus.COMPLETED, from, to);
        long totalPages = printJobRepository.sumTotalPagesByShopIdAndStatusCompletedAndCreatedAtBetween(shopId, from, to);
        java.math.BigDecimal totalRevenew = printJobRepository.sumEstimatedCostByShopIdAndStatusCompletedAndCreatedAtBetween(shopId, from, to);
        
        return com.printease.backend.dto.response.ShopFileStatsResponse.builder()
                .totalJobs(totalJobs)
                .totalRevenew(totalRevenew)
                .completedJobs(completedJobs)
                .totalPages(totalPages)
                .build();
    }
}
