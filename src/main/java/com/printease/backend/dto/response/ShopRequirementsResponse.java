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
public class ShopRequirementsResponse {

    private List<String> acceptedFormats;
    private Integer maxFileSizeMb;
    private Integer maxFilesPerJob;
    private BigDecimal pricePerPageBW;
    private BigDecimal pricePerPageColor;
}
