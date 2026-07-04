package com.printease.backend.service;

import com.printease.backend.exception.BadRequestException;
import com.printease.backend.exception.ResourceNotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "minio")
@Slf4j
public class MinioFileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioFileStorageServiceImpl(MinioClient minioClient,
                                       @Value("${app.storage.minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Minio bucket '{}' created successfully.", bucketName);
            } else {
                log.info("Minio bucket '{}' already exists.", bucketName);
            }
        } catch (Exception e) {
            log.error("Error occurred while checking/creating bucket: ", e);
            throw new RuntimeException("Could not initialize Minio storage", e);
        }
    }

    @Override
    public String store(MultipartFile file, String shopSlug, UUID jobId) {
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String objectName = shopSlug + "/" + jobId.toString() + "/" + UUID.randomUUID() + "_" + originalFilename;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .build()
            );
            return objectName;
        } catch (Exception e) {
            log.error("Error occurred while uploading file: ", e);
            throw new BadRequestException("Failed to store file in MinIO: " + originalFilename);
        }
    }

    @Override
    public Resource loadAsResource(String path) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
            return new InputStreamResource(stream);
        } catch (Exception e) {
            log.error("Error occurred while getting file {}: ", path, e);
            throw new ResourceNotFoundException("File", "path", path);
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to delete file from MinIO at path {}: {}", path, e.getMessage());
        }
    }

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
