package com.printease.backend.service;

import com.printease.backend.dto.request.FileOptionRequest;
import com.printease.backend.dto.request.JobUpdateRequest;
import com.printease.backend.dto.response.DashboardStatsResponse;
import com.printease.backend.dto.response.PrintFileResponse;
import com.printease.backend.dto.response.PrintJobResponse;
import com.printease.backend.entity.PrintFile;
import com.printease.backend.entity.PrintJob;
import com.printease.backend.entity.Shop;
import com.printease.backend.entity.ShopRequirements;
import com.printease.backend.entity.enums.JobStatus;
import com.printease.backend.exception.BadRequestException;
import com.printease.backend.exception.ResourceNotFoundException;
import com.printease.backend.repository.PrintFileRepository;
import com.printease.backend.repository.PrintJobRepository;
import com.printease.backend.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrintJobService {

    private final PrintJobRepository printJobRepository;
    private final PrintFileRepository printFileRepository;
    private final ShopRepository shopRepository;
    private final FileStorageService fileStorageService;
    private final TokenGeneratorService tokenGeneratorService;
    private final PricingService pricingService;
    private final DocumentProcessingService documentProcessingService;

    @Transactional
    public PrintJobResponse submitJob(String shopSlug,
                                      String specialInstructions,
                                      List<FileOptionRequest> options,
                                      List<MultipartFile> files) {

        Shop shop = shopRepository.findBySlugAndIsApprovedTrueAndIsActiveTrue(shopSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "slug", shopSlug));

        ShopRequirements req = shop.getRequirements();
        if (req == null) {
            throw new BadRequestException("Shop requirements not configured");
        }

        // Validate files against shop requirements
        List<String> errors = pricingService.validateFiles(files, req);
        if (!errors.isEmpty()) {
            throw new BadRequestException("File validation failed: " + String.join("; ", errors));
        }

        String accessToken = tokenGeneratorService.generateTokenForShop(shop.getId());

        PrintJob job = PrintJob.builder()
                .accessToken(accessToken)
                .shop(shop)
                .specialInstructions(specialInstructions)
                .status(JobStatus.PENDING)
                .build();
        job = printJobRepository.save(job);


        int totalPages = 0;
        BigDecimal estimatedCost = BigDecimal.ZERO;
        List<PrintFile> printFiles = new ArrayList<>();

        int fileIndex = 0;
        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "unnamed";
            
            FileOptionRequest opt = (options != null && fileIndex < options.size())
                    ? options.get(fileIndex)
                    : new FileOptionRequest();
            fileIndex++;

            String storedPath = fileStorageService.store(file, shop.getSlug(), job.getId());

            int autoCalculatedPageCount = documentProcessingService.calculatePageCount(file);
            int pageCount = Math.max(autoCalculatedPageCount, 1);
            int copies = Math.max(opt.getCopies(), 1);

            PrintFile pf = PrintFile.builder()
                    .job(job)
                    .originalName(originalName)
                    .sizeBytes(file.getSize())
                    .mimeType(file.getContentType() != null
                            ? file.getContentType() : "application/octet-stream")
                    .filePath(storedPath)
                    .pageCount(pageCount)
                    .colorPrint(opt.isColorPrint())
                    .copies(copies)
                    .doubleSided(opt.isDoubleSided())
                    .build();
            printFiles.add(pf);

            totalPages += pageCount * copies;
            estimatedCost = estimatedCost.add(
                    pricingService.calculateCost(pageCount, copies, opt.isColorPrint(), req));
        }

        printFileRepository.saveAll(printFiles);
        job.setTotalPages(totalPages);
        job.setEstimatedCost(estimatedCost);
        job.setFiles(printFiles);
        job = printJobRepository.save(job);

        log.info("Job submitted: {} (token: {}, shop: {})",
                job.getId(), accessToken, shopSlug);
        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public PrintJobResponse getJobByShopAndToken(String shopSlug, String token) {
        PrintJob job = printJobRepository.findFirstByShopSlugAndAccessTokenOrderByCreatedAtDesc(shopSlug, token.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintJob", "accessToken", token));
        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public PrintJobResponse getJobByShopIdAndToken(UUID shopId, String token) {
        PrintJob job = printJobRepository.findFirstByShopIdAndAccessTokenOrderByCreatedAtDesc(shopId, token.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintJob", "accessToken", token));
        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(UUID shopId) {
        long totalJobs = printJobRepository.countByShopId(shopId);
        long totalPagesPrinted = printJobRepository
                .sumTotalPagesByShopIdAndStatusCompleted(shopId);
        BigDecimal totalRevenue = printJobRepository
                .sumEstimatedCostByShopIdAndStatusCompleted(shopId);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        List<PrintJob> recent = printJobRepository
                .findTop10ByShopIdOrderByCreatedAtDesc(shopId);

        return DashboardStatsResponse.builder()
                .totalJobs(totalJobs)
                .totalPagesPrinted(totalPagesPrinted)
                .totalRevenue(totalRevenue)
                .recentJobs(recent.stream().map(this::toJobResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<PrintJobResponse> getJobsByShop(UUID shopId, JobStatus status,
                                                 String query, Pageable pageable) {
        return printJobRepository
                .findByShopIdWithFilters(shopId, status, query, pageable)
                .map(this::toJobResponse);
    }

    @Transactional(readOnly = true)
    public PrintJobResponse getJobById(UUID jobId) {
        PrintJob job = printJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintJob", "id", jobId));
        return toJobResponse(job);
    }

    @Transactional
    public PrintJobResponse updateJob(UUID jobId, JobUpdateRequest request,
                                       UUID adminShopId) {
        PrintJob job = printJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintJob", "id", jobId));

        if (!job.getShop().getId().equals(adminShopId)) {
            throw new BadRequestException("This job does not belong to your shop");
        }

        // Status transition validation
        if (request.getStatus() != null) {
            JobStatus newStatus;
            try {
                newStatus = JobStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + request.getStatus());
            }
            if (!job.getStatus().canTransitionTo(newStatus)) {
                throw new BadRequestException("Invalid status transition: "
                        + job.getStatus() + " → " + newStatus);
            }

            if (java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS).isAfter(job.getCreatedAt())) {
                throw new BadRequestException("token expired");
            }

            job.setStatus(newStatus);
        }



        // Override rules (section 6c)
        if (request.getTotalPages() != null) {
            job.setTotalPages(request.getTotalPages());
            if (request.getEstimatedCost() != null) {
                job.setEstimatedCost(request.getEstimatedCost());
            } else {
                ShopRequirements req = job.getShop().getRequirements();
                if (req != null) {
                    job.setEstimatedCost(pricingService.recalculateCost(
                            request.getTotalPages(), req));
                }
            }
        } else if (request.getEstimatedCost() != null) {
            job.setEstimatedCost(request.getEstimatedCost());
        }

        job = printJobRepository.save(job);
        log.info("Job updated: {}", jobId);
        return toJobResponse(job);
    }

    @Transactional(readOnly = true)
    public UUID getShopIdForJob(UUID jobId) {
        PrintJob job = printJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintJob", "id", jobId));
        return job.getShop().getId();
    }

    @Transactional(readOnly = true)
    public PrintFile getFileWithOwnership(UUID fileId, UUID shopId) {
        PrintFile printFile = printFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PrintFile", "id", fileId));
                        
        if (shopId != null && !printFile.getJob().getShop().getId().equals(shopId)) {
            throw new AccessDeniedException("You do not have access to this file");
        }
        
        return printFile;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadFile(UUID fileId, UUID shopId, String action) {
        PrintFile printFile = getFileWithOwnership(fileId, shopId);
        
        if (java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS).isAfter(printFile.getJob().getCreatedAt())) {
            throw new BadRequestException("File has expired and is no longer available.");
        }

        Resource resource = fileStorageService.loadAsResource(printFile.getFilePath());

        String contentType = printFile.getMimeType() != null
                ? printFile.getMimeType()
                : "application/octet-stream";

        String dispositionType = "print".equalsIgnoreCase(action) || "view".equalsIgnoreCase(action) 
                ? "inline" 
                : "attachment";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        dispositionType + "; filename=\"" + printFile.getOriginalName() + "\"")
                .body(resource);
    }

    // ===== DTO Mapping =====

    private PrintJobResponse toJobResponse(PrintJob job) {
        Map<String, Object> printOptions = new HashMap<>();
        printOptions.put("specialInstructions", job.getSpecialInstructions());

        List<PrintFileResponse> fileResponses = Collections.emptyList();
        if (job.getFiles() != null) {
            fileResponses = job.getFiles().stream()
                    .map(this::toFileResponse)
                    .collect(Collectors.toList());
        }

        Boolean colorPrint = null;
        Integer copies = null;
        Boolean doubleSided = null;
        
        if (job.getFiles() != null && !job.getFiles().isEmpty()) {
            PrintFile firstFile = job.getFiles().get(0);
            colorPrint = firstFile.getColorPrint();
            copies = firstFile.getCopies();
            doubleSided = firstFile.getDoubleSided();
        }

        return PrintJobResponse.builder()
                .id(job.getId().toString())
                .accessToken(job.getAccessToken())
                .status(job.getStatus().name())
                .shopId(job.getShop() != null
                        ? job.getShop().getId().toString() : null)
                .shopName(job.getShop() != null
                        ? job.getShop().getName() : null)
                .printOptions(printOptions)
                .colorPrint(colorPrint)
                .copies(copies)
                .doubleSided(doubleSided)
                .totalPages(job.getTotalPages())
                .estimatedCost(job.getEstimatedCost())
                .files(fileResponses)
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private PrintFileResponse toFileResponse(PrintFile file) {
        String fileType = file.getMimeType() != null
                && file.getMimeType().startsWith("image/")
                ? "IMAGE" : "DOCUMENT";

        return PrintFileResponse.builder()
                .id(file.getId().toString())
                .originalName(file.getOriginalName())
                .sizeBytes(file.getSizeBytes())
                .mimeType(file.getMimeType())
                .pageCount(file.getPageCount())
                .fileType(fileType)
                .colorPrint(file.getColorPrint())
                .copies(file.getCopies())
                .doubleSided(file.getDoubleSided())
                .createdAt(file.getJob() != null ? file.getJob().getCreatedAt() : file.getCreatedAt())
                .updatedAt(file.getJob() != null ? file.getJob().getUpdatedAt() : file.getUpdatedAt())
                .build();
    }
}
