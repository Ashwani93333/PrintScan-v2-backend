package com.printease.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintFileResponse {

    private String id;
    private String originalName;
    private Long sizeBytes;
    private String mimeType;
    private Integer pageCount;
    private String fileType;   // Derived: "IMAGE" if mimeType starts with "image/", else "DOCUMENT"
    private Boolean colorPrint;
    private Integer copies;
    private Boolean doubleSided;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
}
