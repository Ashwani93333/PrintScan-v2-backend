package com.printease.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Abstraction for file storage — swap implementations via @Profile.
 */
public interface FileStorageService {

    /**
     * Store a file and return the relative path.
     */
    String store(MultipartFile file, String shopSlug, UUID jobId);

    /**
     * Load a file as a Spring Resource for streaming.
     */
    Resource loadAsResource(String path);

    /**
     * Delete a file by its stored path.
     */
    void delete(String path);
}
