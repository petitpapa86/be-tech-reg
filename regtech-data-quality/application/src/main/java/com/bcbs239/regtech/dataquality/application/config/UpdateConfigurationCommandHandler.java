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
 * Command handler for updating data quality configuration.
 * 
 * <p>CQRS Pattern: Handles WRITE operations for configuration.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Validate threshold values</li>
 *   <li>Persist changes via QualityThresholdRepository</li>
 *   <li>Return updated configuration</li>
 * </ul>
 */
@Component
public class UpdateConfigurationCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateConfigurationCommandHandler.class);
    
    private final QualityThresholdRepository thresholdRepository;
    private final GetConfigurationQueryHandler queryHandler;
    
    public UpdateConfigurationCommandHandler(
        QualityThresholdRepository thresholdRepository,
        GetConfigurationQueryHandler queryHandler
    ) {
        this.thresholdRepository = thresholdRepository;
        this.queryHandler = queryHandler;
    }
    
    @Observed(name = "config.command.update", contextualName = "Update Configuration Command")
    public Result<ConfigurationDto> handle(UpdateConfigurationCommand command) {
        logger.info("Handling UpdateConfigurationCommand for bankId: {}", command.bankId());
        
        try {
            // Validate thresholds
            Result<Void> validation = validateThresholds(command.configuration().thresholds());
            if (validation.isFailure()) {
                return Result.failure(validation.getError());
            }
            
            // Persist thresholds
            QualityThreshold threshold = new QualityThreshold(
                command.bankId(),
                command.configuration().thresholds().completenessMinPercent(),
                command.configuration().thresholds().accuracyMaxErrorPercent(),
                command.configuration().thresholds().timelinessDays(),
                command.configuration().thresholds().consistencyPercent()
            );
            
            thresholdRepository.save(threshold);
            
            logger.info("Successfully updated configuration for bankId: {}", command.bankId());
            
            // Return updated configuration via query handler
            return queryHandler.handle(new GetConfigurationQuery(command.bankId()));
            
        } catch (Exception e) {
            logger.error("Failed to update configuration for bankId: {}", command.bankId(), e);
            return Result.failure(
                ErrorDetail.of(
                    "CONFIG_UPDATE_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to update configuration",
                    "config.update.failed"
                )
            );
        }
    }
    
    private Result<Void> validateThresholds(ConfigurationDto.ThresholdsDto thresholds) {
        if (thresholds.completenessMinPercent() < 0 || thresholds.completenessMinPercent() > 100) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_COMPLETENESS_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Completeness threshold must be between 0 and 100",
                    "validation.completeness.range"
                )
            );
        }
        
        if (thresholds.accuracyMaxErrorPercent() < 0 || thresholds.accuracyMaxErrorPercent() > 100) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_ACCURACY_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Accuracy threshold must be between 0 and 100",
                    "validation.accuracy.range"
                )
            );
        }
        
        if (thresholds.timelinessDays() < 0) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_TIMELINESS_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Timeliness threshold must be positive",
                    "validation.timeliness.positive"
                )
            );
        }
        
        if (thresholds.consistencyPercent() < 0 || thresholds.consistencyPercent() > 100) {
            return Result.failure(
                ErrorDetail.of(
                    "INVALID_CONSISTENCY_THRESHOLD",
                    ErrorType.VALIDATION_ERROR,
                    "Consistency threshold must be between 0 and 100",
                    "validation.consistency.range"
                )
            );
        }
        
        return Result.success(null);
    }
}
