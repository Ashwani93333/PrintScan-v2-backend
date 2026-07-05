package com.printease.backend.service;

import com.printease.backend.dto.response.AnalyticsResponse;
import com.printease.backend.entity.PrintJob;
import com.printease.backend.entity.Shop;
import com.printease.backend.entity.enums.JobStatus;
import com.printease.backend.repository.PrintJobRepository;
import com.printease.backend.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ShopRepository shopRepository;
    private final PrintJobRepository printJobRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(Instant from, Instant to) {
        log.info("Fetching global analytics | from={} | to={}", from, to);
        long totalShops = shopRepository.count();
        long approvedShops = shopRepository.countByIsApprovedTrue();
        long totalJobs = printJobRepository.countByCreatedAtBetween(from, to);
        long completedJobs = printJobRepository.countByStatus(JobStatus.COMPLETED);
        long cancelledJobs = printJobRepository.countByStatus(JobStatus.CANCELLED);

        List<PrintJob> jobsInRange = printJobRepository.findAllByCreatedAtBetween(from, to);
        log.info("Analytics data | totalShops={} | approvedShops={} | totalJobs={} | completed={} | cancelled={} | jobsInRange={}",
                totalShops, approvedShops, totalJobs, completedJobs, cancelledJobs, jobsInRange.size());

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

        log.info("Analytics complete | {} date entries | {} shop rates computed", jobsByDate.size(), shopRates.size());
        return AnalyticsResponse.builder()
                .totalShops(totalShops)
                .approvedShops(approvedShops)
                .totalJobs(totalJobs)
                .completedJobs(completedJobs)
                .cancelledJobs(cancelledJobs)
                .jobsByDate(jobsByDate)
                .shopCompletionRates(shopRates)
                .build();
    }

    @Transactional(readOnly = true)
    public com.printease.backend.dto.response.ShopFileStatsResponse getShopFileStats(UUID shopId, Instant from, Instant to) {
        log.info("Fetching shop file stats | shopId={} | from={} | to={}", shopId, from, to);
        long totalJobs = printJobRepository.countByShopIdAndCreatedAtBetween(shopId, from, to);
        long completedJobs = printJobRepository.countByShopIdAndStatusAndCreatedAtBetween(shopId, JobStatus.COMPLETED, from, to);
        long totalPages = printJobRepository.sumTotalPagesByShopIdAndStatusCompletedAndCreatedAtBetween(shopId, from, to);
        java.math.BigDecimal totalRevenew = printJobRepository.sumEstimatedCostByShopIdAndStatusCompletedAndCreatedAtBetween(shopId, from, to);
        
        log.info("Shop file stats complete | shopId={} | totalJobs={} | completed={} | pages={} | revenue={}",
                shopId, totalJobs, completedJobs, totalPages, totalRevenew);
        return com.printease.backend.dto.response.ShopFileStatsResponse.builder()
                .totalJobs(totalJobs)
                .totalRevenew(totalRevenew)
                .completedJobs(completedJobs)
                .totalPages(totalPages)
                .build();
    }
}
