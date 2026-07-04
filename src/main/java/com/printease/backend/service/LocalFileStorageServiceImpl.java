package com.printease.backend.service;

import com.printease.backend.exception.BadRequestException;
import com.printease.backend.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local filesystem implementation of FileStorageService.
 * Files organized as: {root}/{shopSlug}/{jobId}/{uuid}_{sanitizedFilename}
 *
 * // TODO: swap for S3FileStorageServiceImpl in production if needed
 * //       Add @Profile("s3") to an S3 impl and @Profile("!s3") here
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageServiceImpl(@Value("${app.storage.location:./uploads}") String storageLocation) {
        this.rootLocation = Paths.get(storageLocation).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory: " + storageLocation, ex);
        }
    }

    @Override
    public String store(MultipartFile file, String shopSlug, UUID jobId) {
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + originalFilename;
        Path targetDir = rootLocation.resolve(shopSlug).resolve(jobId.toString());

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path from root
            return rootLocation.relativize(targetPath).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store file: " + originalFilename + " — " + ex.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String path) {
        try {
            Path filePath = rootLocation.resolve(path).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File", "path", path);
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File", "path", path);
        }
    }

    @Override
    public void delete(String path) {
        try {
            Path filePath = rootLocation.resolve(path).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Failed to delete file at path {}: {}", path, ex.getMessage());
        }
    }

    /**
     * Strip path traversal characters from filenames.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }
        return filename
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\.\\.", "_")
                .trim();
    }
}
