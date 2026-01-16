package com.bcbs239.regtech.reportgeneration.application.configuration;

import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfiguration;
import com.bcbs239.regtech.reportgeneration.domain.configuration.ReportConfigurationRepository;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(GetReportConfigurationHandler.class);
    
    private final ReportConfigurationRepository repository;
    
    @Transactional(readOnly = true)
    public Maybe<ReportConfiguration> handle(Long bankId) {
        logger.info("GetReportConfigurationHandler.handle start | bankId={}", bankId);
        Maybe<ReportConfiguration> result = repository.findByBankId(bankId);
        logger.debug("GetReportConfigurationHandler.handle result present={}", result.isPresent());
        logger.info("GetReportConfigurationHandler.handle end | bankId={}", bankId);
        return result;
    }
}
