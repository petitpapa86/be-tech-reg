package com.bcbs239.regtech.reportgeneration.infrastructure.persistence.configuration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface ReportConfigurationJpaRepository 
        extends JpaRepository<ReportConfigurationJpaEntity, Long> {
    
    /**
     * Get the configuration for a specific bank
     */
    Optional<ReportConfigurationJpaEntity> findByBankId(Long bankId);
}
