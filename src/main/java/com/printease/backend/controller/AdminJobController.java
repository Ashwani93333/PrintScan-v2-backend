package com.printease.backend.controller;

import com.printease.backend.dto.request.JobUpdateRequest;
import com.printease.backend.dto.response.PrintJobResponse;
import com.printease.backend.entity.enums.JobStatus;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.PrintJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminJobController {

    private final PrintJobService printJobService;

    @GetMapping("/shops/{shopId}/jobs")
    public ResponseEntity<Page<PrintJobResponse>> getJobs(
            @PathVariable UUID shopId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info(">>> GET /api/admin/shops/{}/jobs | status={} | query={} | page={} | size={} | user={}",
                shopId, status, query, page, size, principal.getEmail());
        verifyShopOwnership(principal, shopId);

        JobStatus jobStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                jobStatus = JobStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                log.warn("Invalid job status filter ignored: '{}'", status);
                // If invalid status, just ignore the filter
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PrintJobResponse> result = printJobService.getJobsByShop(shopId, jobStatus, query, pageable);
        log.info("<<< GET /api/admin/shops/{}/jobs | SUCCESS | totalElements={} | totalPages={}",
                shopId, result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<PrintJobResponse> getJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info(">>> GET /api/admin/jobs/{} | user={}", jobId, principal.getEmail());
        // Verify this job belongs to the admin's shop
        UUID jobShopId = printJobService.getShopIdForJob(jobId);
        verifyShopOwnership(principal, jobShopId);

        PrintJobResponse response = printJobService.getJobById(jobId);
        log.info("<<< GET /api/admin/jobs/{} | SUCCESS | status={}", jobId, response.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shops/{shopId}/jobs/token/{token}")
    public ResponseEntity<PrintJobResponse> getJobByToken(
            @PathVariable UUID shopId,
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info(">>> GET /api/admin/shops/{}/jobs/token/{} | user={}", shopId, token, principal.getEmail());
        verifyShopOwnership(principal, shopId);
        PrintJobResponse response = printJobService.getJobByShopIdAndToken(shopId, token);
        log.info("<<< GET /api/admin/shops/{}/jobs/token/{} | SUCCESS | jobId={}", shopId, token, response.getId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/jobs/{jobId}")
    public ResponseEntity<PrintJobResponse> updateJob(
            @PathVariable UUID jobId,
            @RequestBody JobUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info(">>> PATCH /api/admin/jobs/{} | newStatus={} | user={}", jobId, request.getStatus(), principal.getEmail());
        PrintJobResponse response = printJobService.updateJob(jobId, request, principal.getShopId());
        log.info("<<< PATCH /api/admin/jobs/{} | SUCCESS | status={}", jobId, response.getStatus());
        return ResponseEntity.ok(response);
    }

    private void verifyShopOwnership(UserPrincipal principal, UUID shopId) {
        if (principal.getShopId() == null || !principal.getShopId().equals(shopId)) {
            log.warn("ACCESS DENIED | user={} tried to access shopId={} but owns shopId={}",
                    principal.getEmail(), shopId, principal.getShopId());
            throw new AccessDeniedException("You do not have access to this shop");
        }
    }
}
