package com.printease.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalJobs;
    private long totalPagesPrinted;
    private BigDecimal totalRevenue;
    private List<PrintJobResponse> recentJobs;
}
