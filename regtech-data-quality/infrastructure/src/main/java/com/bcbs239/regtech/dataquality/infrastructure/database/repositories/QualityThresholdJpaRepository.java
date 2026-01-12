package com.bcbs239.regtech.dataquality.infrastructure.database.repositories;

import com.bcbs239.regtech.dataquality.infrastructure.database.entities.QualityThresholdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualityThresholdJpaRepository extends JpaRepository<QualityThresholdEntity, Long> {
    Optional<QualityThresholdEntity> findFirstByBankIdAndIsActiveTrueOrderByCreatedAtDesc(String bankId);
}
