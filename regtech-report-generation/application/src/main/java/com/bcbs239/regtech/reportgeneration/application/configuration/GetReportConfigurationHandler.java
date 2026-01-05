package com.bcbs239.regtech.reportgeneration.application.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfigurationRepository;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query Handler: Get Report Configuration
 * 
 * Application layer = GLUE only
 */
@Service
@RequiredArgsConstructor
public class GetReportConfigurationHandler {
    
    private final ReportConfigurationRepository repository;
    
    @Transactional(readOnly = true)
    public Maybe<ReportConfiguration> handle(Long bankId) {
        return repository.findByBankId(bankId);
    }
}
