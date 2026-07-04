package com.printease.backend.repository;

import com.printease.backend.entity.ShopRequirements;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ShopRequirementsRepository extends JpaRepository<ShopRequirements, UUID> {
}
