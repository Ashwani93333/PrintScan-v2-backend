package com.printease.backend.controller;

import com.printease.backend.entity.PrintFile;
import com.printease.backend.security.UserPrincipal;
import com.printease.backend.service.FileStorageService;
import com.printease.backend.service.PrintJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminFileController {

    private final PrintJobService printJobService;

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable UUID fileId,
            @RequestParam(value = "action", required = false, defaultValue = "download") String action,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info(">>> GET /api/admin/files/{}/download | action={} | user={} | shopId={}",
                fileId, action, principal.getEmail(), principal.getShopId());
        
        ResponseEntity<Resource> response = printJobService.downloadFile(fileId, principal.getShopId(), action);
        log.info("<<< GET /api/admin/files/{}/download | SUCCESS | action={}", fileId, action);
        return response;
    }
}
