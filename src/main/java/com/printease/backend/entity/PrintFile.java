package com.printease.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "print_files", indexes = {
        @Index(name = "idx_print_files_job_id", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrintFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private PrintJob job;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @Column(name = "color_print", nullable = false)
    @Builder.Default
    private Boolean colorPrint = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer copies = 1;

    @Column(name = "double_sided", nullable = false)
    @Builder.Default
    private Boolean doubleSided = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private java.time.Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.Instant updatedAt;
}
