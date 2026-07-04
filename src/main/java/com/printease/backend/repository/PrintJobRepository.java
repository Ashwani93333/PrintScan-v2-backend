package com.printease.backend.repository;

import com.printease.backend.entity.PrintJob;
import com.printease.backend.entity.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrintJobRepository extends JpaRepository<PrintJob, UUID> {

    boolean existsByAccessToken(String accessToken);

    Optional<PrintJob> findFirstByShopSlugAndAccessTokenOrderByCreatedAtDesc(String slug, String accessToken);

    Optional<PrintJob> findFirstByShopIdAndAccessTokenOrderByCreatedAtDesc(UUID shopId, String accessToken);

    long countByShopIdAndCreatedAtBetween(UUID shopId, Instant from, Instant to);

    Page<PrintJob> findByShopId(UUID shopId, Pageable pageable);

//    @Query("SELECT j FROM PrintJob j WHERE j.shop.id = :shopId " +
//            "AND (:status IS NULL OR j.status = :status) " +
//            "AND (:query IS NULL OR LOWER(j.accessToken) LIKE LOWER(CONCAT('%', :query, '%')) " +
//            "OR LOWER(j.customerName) LIKE LOWER(CONCAT('%', :query, '%')) " +
//            "OR LOWER(j.customerPhone) LIKE LOWER(CONCAT('%', :query, '%')))")
//    Page<PrintJob> findByShopIdWithFilters(
//            @Param("shopId") UUID shopId,
//            @Param("status") JobStatus status,
//            @Param("query") String query,
//            Pageable pageable);
//
//    @Query("""
//SELECT j
//FROM PrintJob j
//WHERE j.shop.id = :shopId
//AND (:status IS NULL OR j.status = :status)
//AND (
//    :query IS NULL
//    OR LOWER(j.accessToken) LIKE LOWER(CONCAT('%', :query, '%'))
//)
//""")
//    Page<PrintJob> findByShopIdWithFilters(
//            @Param("shopId") UUID shopId,
//            @Param("status") JobStatus status,
//            @Param("query") String query,
//            Pageable pageable);

    //byte issue with accessToken-- solved as string to converted
    @Query("""
SELECT j
FROM PrintJob j
WHERE j.shop.id = :shopId
AND (:status IS NULL OR j.status = :status)
AND (
    :query IS NULL
    OR LOWER(j.accessToken) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))
)
""")
    Page<PrintJob> findByShopIdWithFilters(
            @Param("shopId") UUID shopId,
            @Param("status") JobStatus status,
            @Param("query") String query,
            Pageable pageable);

    long countByShopId(UUID shopId);

    long countByShopIdAndStatus(UUID shopId, JobStatus status);

    @Query("SELECT COALESCE(SUM(j.totalPages), 0) FROM PrintJob j WHERE j.shop.id = :shopId AND j.status = 'COMPLETED'")
    long sumTotalPagesByShopIdAndStatusCompleted(@Param("shopId") UUID shopId);

    @Query("SELECT COALESCE(SUM(j.estimatedCost), 0) FROM PrintJob j WHERE j.shop.id = :shopId AND j.status = 'COMPLETED'")
    BigDecimal sumEstimatedCostByShopIdAndStatusCompleted(@Param("shopId") UUID shopId);

    long countByShopIdAndStatusAndCreatedAtBetween(UUID shopId, JobStatus status, Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(j.totalPages), 0) FROM PrintJob j WHERE j.shop.id = :shopId AND j.status = 'COMPLETED' AND j.createdAt BETWEEN :from AND :to")
    long sumTotalPagesByShopIdAndStatusCompletedAndCreatedAtBetween(@Param("shopId") UUID shopId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COALESCE(SUM(j.estimatedCost), 0) FROM PrintJob j WHERE j.shop.id = :shopId AND j.status = 'COMPLETED' AND j.createdAt BETWEEN :from AND :to")
    BigDecimal sumEstimatedCostByShopIdAndStatusCompletedAndCreatedAtBetween(@Param("shopId") UUID shopId, @Param("from") Instant from, @Param("to") Instant to);

    List<PrintJob> findTop10ByShopIdOrderByCreatedAtDesc(UUID shopId);

    // Analytics queries for Super Admin
    long countByStatus(JobStatus status);

    @Query("SELECT j FROM PrintJob j WHERE j.createdAt BETWEEN :from AND :to")
    List<PrintJob> findAllByCreatedAtBetween(@Param("from") Instant from, @Param("to") Instant to);

    long countByCreatedAtBetween(Instant from, Instant to);
}
