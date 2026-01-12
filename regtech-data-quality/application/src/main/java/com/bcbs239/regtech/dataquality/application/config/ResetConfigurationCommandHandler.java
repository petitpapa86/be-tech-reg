package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Command handler for resetting configuration to system defaults.
 * 
 * <p>CQRS Pattern: Handles WRITE operations for configuration reset.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Load default threshold values</li>
 *   <li>Persist defaults via QualityThresholdRepository</li>
 *   <li>Return default configuration</li>
 * </ul>
 */
@Component
public class ResetConfigurationCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ResetConfigurationCommandHandler.class);
    
    private final QualityThresholdRepository thresholdRepository;
    private final GetConfigurationQueryHandler queryHandler;
    
    public ResetConfigurationCommandHandler(
        QualityThresholdRepository thresholdRepository,
        GetConfigurationQueryHandler queryHandler
    ) {
        this.thresholdRepository = thresholdRepository;
        this.queryHandler = queryHandler;
    }
    
    @Observed(name = "config.command.reset", contextualName = "Reset Configuration Command")
    public Result<ConfigurationDto> handle(ResetConfigurationCommand command) {
        logger.info("Handling ResetConfigurationCommand for bankId: {}", command.bankId());
        
        try {
            QualityThreshold defaultThreshold = getDefaultThresholds(command.bankId());
            thresholdRepository.save(defaultThreshold);
            
            logger.info("Successfully reset configuration for bankId: {}", command.bankId());
            
            // Return configuration via query handler
            return queryHandler.handle(new GetConfigurationQuery(command.bankId()));
            
        } catch (Exception e) {
            logger.error("Failed to reset configuration for bankId: {}", command.bankId(), e);
            return Result.failure(
                ErrorDetail.of(
                    "CONFIG_RESET_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to reset configuration",
                    "config.reset.failed"
                )
            );
        }
    }
    
    private QualityThreshold getDefaultThresholds(String bankId) {
        return new QualityThreshold(
            bankId,
            95.0,  // completenessMinPercent
            5.0,   // accuracyMaxErrorPercent
            7,     // timelinessDays
            98.0   // consistencyPercent
        );
    }
}
