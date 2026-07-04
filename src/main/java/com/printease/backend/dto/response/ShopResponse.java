package com.printease.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopResponse {

    private String id;
    private String name;
    private String slug;
    private String address;
    private String phone;
    private String email;
    private String description;
    private Boolean isActive;
    private Boolean isApproved;
    private String adminId;
    private String adminEmail;
    private String adminName;
    private Instant createdAt;
    private Integer qrVisits;
    private String qrCode;
    private ShopRequirementsResponse requirements;
}
