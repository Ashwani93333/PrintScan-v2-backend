package com.printease.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shop_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopRequirements {

    @Id
    @Column(name = "shop_id")
    private UUID shopId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Column(name = "accepted_formats")
    @Builder.Default
    private String acceptedFormats = "PDF,JPG,PNG,DOCX";

    @Column(name = "max_file_size_mb")
    @Builder.Default
    private Integer maxFileSizeMb = 25;

    @Column(name = "max_files_per_job")
    @Builder.Default
    private Integer maxFilesPerJob = 5;

    @Column(name = "price_per_page_bw", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerPageBw = new BigDecimal("2.00");

    @Column(name = "price_per_page_color", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pricePerPageColor = new BigDecimal("10.00");
}
