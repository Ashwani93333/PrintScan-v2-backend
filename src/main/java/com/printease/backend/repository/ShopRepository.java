package com.printease.backend.repository;

import com.printease.backend.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    Optional<Shop> findBySlug(String slug);

    Optional<Shop> findBySlugAndIsApprovedTrue(String slug);

    Optional<Shop> findByAdminId(UUID adminId);

    boolean existsBySlug(String slug);

    boolean existsByAdminId(UUID adminId);

    long countByIsApprovedTrue();

    Page<Shop> findAll(Pageable pageable);
}
