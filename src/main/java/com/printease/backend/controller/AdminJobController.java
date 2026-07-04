package com.printease.backend.controller;

import com.printease.backend.dto.request.JobUpdateRequest;
import com.printease.backend.dto.response.PrintJobResponse;
import com.printease.backend.entity.enums.JobStatus;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.PrintJobService;
import lombok.RequiredArgsConstructor;
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

        verifyShopOwnership(principal, shopId);

        JobStatus jobStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                jobStatus = JobStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // If invalid status, just ignore the filter
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(printJobService.getJobsByShop(shopId, jobStatus, query, pageable));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<PrintJobResponse> getJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Verify this job belongs to the admin's shop
        UUID jobShopId = printJobService.getShopIdForJob(jobId);
        verifyShopOwnership(principal, jobShopId);

        return ResponseEntity.ok(printJobService.getJobById(jobId));
    }

    @GetMapping("/shops/{shopId}/jobs/token/{token}")
    public ResponseEntity<PrintJobResponse> getJobByToken(
            @PathVariable UUID shopId,
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal principal) {
        verifyShopOwnership(principal, shopId);
        return ResponseEntity.ok(printJobService.getJobByShopIdAndToken(shopId, token));
    }

    @PatchMapping("/jobs/{jobId}")
    public ResponseEntity<PrintJobResponse> updateJob(
            @PathVariable UUID jobId,
            @RequestBody JobUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(printJobService.updateJob(jobId, request, principal.getShopId()));
    }

    private void verifyShopOwnership(UserPrincipal principal, UUID shopId) {
        if (principal.getShopId() == null || !principal.getShopId().equals(shopId)) {
            throw new AccessDeniedException("You do not have access to this shop");
        }
    }
}
