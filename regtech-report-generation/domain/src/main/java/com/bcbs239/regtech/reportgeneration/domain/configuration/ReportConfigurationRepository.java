package com.bcbs239.regtech.reportgeneration.domain.configuration;

import com.bcbs239.regtech.core.domain.shared.Maybe;

/**
 * Repository interface (domain layer)
 * Implementation in infrastructure layer
 */
public interface ReportConfigurationRepository {
    
    /**
     * Get report configuration by bank ID
     */
    Maybe<ReportConfiguration> findByBankId(Long bankId);
    
    /**
     * Save/update report configuration
     */
    ReportConfiguration save(ReportConfiguration configuration);
}
