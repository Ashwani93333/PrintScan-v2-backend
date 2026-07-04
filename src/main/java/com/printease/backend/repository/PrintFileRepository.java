package com.printease.backend.repository;

import com.printease.backend.entity.PrintFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrintFileRepository extends JpaRepository<PrintFile, UUID> {

    List<PrintFile> findByJobId(UUID jobId);
}
