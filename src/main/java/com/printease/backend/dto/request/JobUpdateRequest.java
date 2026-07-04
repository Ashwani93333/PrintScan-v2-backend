package com.printease.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for PATCH /api/admin/jobs/{jobId}.
 * All fields are optional — only provided fields are applied.
 */
@Data
public class JobUpdateRequest {

    private String status;
    private Integer totalPages;
    private BigDecimal estimatedCost;
}
