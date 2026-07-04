package com.printease.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopFileStatsResponse {
    private long totalJobs;
    private BigDecimal totalRevenew;
    private long completedJobs;
    private long totalPages;
}
