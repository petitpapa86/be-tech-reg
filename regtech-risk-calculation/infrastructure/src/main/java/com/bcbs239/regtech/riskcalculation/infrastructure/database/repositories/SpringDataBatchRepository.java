package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for BatchEntity
 */
@Repository
public interface SpringDataBatchRepository extends JpaRepository<BatchEntity, String> {
}
