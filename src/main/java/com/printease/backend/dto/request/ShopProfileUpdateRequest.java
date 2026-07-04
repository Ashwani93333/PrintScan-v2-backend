package com.printease.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for PUT /api/admin/shops/{shopId}/profile.
 * Supports updating both shop info and nested requirements.
 */
@Data
public class ShopProfileUpdateRequest {

    private String name;
    private String address;
    private String phone;
    private String email;
    private String description;

    private RequirementsUpdate requirements;

    @Data
    public static class RequirementsUpdate {
        private List<String> acceptedFormats;
        private Integer maxFileSizeMb;
        private Integer maxFilesPerJob;
        private BigDecimal pricePerPageBW;
        private BigDecimal pricePerPageColor;
    }
}
