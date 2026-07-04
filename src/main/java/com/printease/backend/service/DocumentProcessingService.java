package com.printease.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class DocumentProcessingService {

    /**
     * Auto-calculates the page count of the uploaded file.
     * For PDFs, it parses the document. For images and other files, it defaults to 1.
     */
    public int calculatePageCount(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return 0;
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        boolean isPdf = (contentType != null && contentType.equalsIgnoreCase("application/pdf")) 
                || (filename != null && filename.toLowerCase().endsWith(".pdf"));

        if (isPdf) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                return document.getNumberOfPages();
            } catch (Exception e) {
                log.error("Failed to parse PDF file {} to determine page count", filename, e);
                // Fallback to 1 if parsing fails
                return 1;
            }
        }

        // For images and other formats, we assume 1 page per file
        return 1;
    }
}
