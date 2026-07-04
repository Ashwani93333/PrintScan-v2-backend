package com.printease.backend.controller;

import com.printease.backend.dto.request.FileOptionRequest;
import com.printease.backend.dto.response.PrintJobResponse;
import com.printease.backend.service.PrintJobService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import java.time.Duration;
import org.springframework.http.ResponseEntity;
import com.printease.backend.exception.BadRequestException;

@RestController
@RequestMapping("/api/public")
@Slf4j
public class PublicJobController {

    private final PrintJobService printJobService;
    private final ObjectMapper objectMapper;
    private final Bucket bucket;

    public PublicJobController(PrintJobService printJobService, ObjectMapper objectMapper) {
        this.printJobService = printJobService;
        this.objectMapper = objectMapper;
        // Allow 20 uploads per minute per shop
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Submit a new print job.
     * Multipart form data: customerName, customerPhone, customerEmail (opt),
     * specialInstructions (opt), options (JSON string array), files (multipart).
     */
    @PostMapping("/shops/{slug}/jobs")
    public ResponseEntity<PrintJobResponse> submitJob(
            @PathVariable String slug,
            @RequestParam(value = "specialInstructions", required = false) String specialInstructions,
            @RequestParam(value = "options", required = false) String optionsJson,
            @RequestParam("files") List<MultipartFile> files) {

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        List<FileOptionRequest> options = Collections.emptyList();
        if (optionsJson != null && !optionsJson.isBlank()) {
            try {
                options = objectMapper.readValue(optionsJson, new TypeReference<List<FileOptionRequest>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse options JSON: {}", e.getMessage());
                throw new BadRequestException("Invalid options JSON format");
            }
        }

        PrintJobResponse response = printJobService.submitJob(
                slug,
                specialInstructions, options, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Fetch a job by its public access token for tracking.
     */
    @GetMapping("/shops/{slug}/jobs/{token}")
    public ResponseEntity<PrintJobResponse> getJobByShopAndToken(@PathVariable String slug, @PathVariable String token) {
        return ResponseEntity.ok(printJobService.getJobByShopAndToken(slug, token));
    }
}
