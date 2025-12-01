package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.PortfolioAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for PortfolioAnalysisEntity
 */
@Repository
public interface SpringDataPortfolioAnalysisRepository extends JpaRepository<PortfolioAnalysisEntity, String> {
    // Inherits findById(String batchId) from JpaRepository
}
