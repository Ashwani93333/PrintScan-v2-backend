package com.printease.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {

    private long totalShops;
    private long approvedShops;
    private long totalJobs;
    private long completedJobs;
    private long cancelledJobs;
    private List<Map<String, Object>> jobsByDate;
    private List<Map<String, Object>> shopCompletionRates;
}
