package com.printease.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    @JsonIgnore
    private String token;
    @JsonIgnore
    @Builder.Default
    private String type = "Bearer";
    
    private String email;
    private String role;
    private String name;
    private String shopId;
    private String shopSlug;
}
