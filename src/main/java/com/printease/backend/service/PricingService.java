package com.printease.backend.service;

import com.printease.backend.entity.Shop;
import com.printease.backend.entity.ShopRequirements;
import com.printease.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    /**
     * Validate uploaded files against shop requirements.
     * Returns a list of validation errors (empty = valid).
     */
    public List<String> validateFiles(List<MultipartFile> files, ShopRequirements req) {
        List<String> errors = new ArrayList<>();

        if (files.size() > req.getMaxFilesPerJob()) {
            errors.add("Too many files: maximum " + req.getMaxFilesPerJob() + " allowed, got " + files.size());
        }

        List<String> acceptedExtensions = List.of(req.getAcceptedFormats().toLowerCase().split(","));
        long maxSizeBytes = req.getMaxFileSizeMb() * 1024L * 1024L;

        for (MultipartFile file : files) {
            String originalName = file.getOriginalFilename();
            if (originalName == null) originalName = "unknown";

            // Check file size
            if (file.getSize() > maxSizeBytes) {
                errors.add("File '" + originalName + "' exceeds maximum size of " + req.getMaxFileSizeMb() + "MB");
            }

            // Check file extension
            String extension = getFileExtension(originalName).toLowerCase();
            if (!acceptedExtensions.contains(extension)) {
                errors.add("File '" + originalName + "' has unsupported format '" + extension
                        + "'. Accepted: " + req.getAcceptedFormats());
            }
        }

        return errors;
    }

    /**
     * Calculate total pages and estimated cost for a job.
     */
    public BigDecimal calculateCost(int pageCount, int copies, boolean colorPrint, ShopRequirements req) {
        int clampedPages = Math.max(pageCount, 1);
        BigDecimal rate = colorPrint ? req.getPricePerPageColor() : req.getPricePerPageBw();
        return rate.multiply(BigDecimal.valueOf((long) clampedPages * copies));
    }

    /**
     * Recalculate estimated cost using BW rate and new total pages.
     */
    public BigDecimal recalculateCost(int totalPages, ShopRequirements req) {
        return req.getPricePerPageBw().multiply(BigDecimal.valueOf(totalPages));
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot + 1);
    }
}
