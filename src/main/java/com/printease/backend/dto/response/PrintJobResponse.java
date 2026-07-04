package com.printease.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJobResponse {

    private String id;
    private String accessToken;
    private String status;
    private String shopId;
    private String shopName;
    private Map<String, Object> printOptions;  // { specialInstructions: "..." }
    private Boolean colorPrint;
    private Integer copies;
    private Boolean doubleSided;
    private Integer totalPages;
    private BigDecimal estimatedCost;
    private List<PrintFileResponse> files;
    private Instant createdAt;
    private Instant updatedAt;
}
