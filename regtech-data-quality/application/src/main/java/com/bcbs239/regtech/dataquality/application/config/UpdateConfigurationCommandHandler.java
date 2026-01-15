package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import com.bcbs239.regtech.dataquality.domain.config.ThresholdPercentage;
import com.bcbs239.regtech.dataquality.domain.config.TimelinessDays;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        
        // Validate thresholds
        Result<Void> validation = validateThresholds(command.configuration().thresholds());
        if (validation.isFailure()) {
            return Result.failure(validation.getError().orElseThrow());
        }
        
        // Persist thresholds
        QualityThreshold threshold = new QualityThreshold(
            command.bankId().value(),
            command.configuration().thresholds().completenessMinPercent(),
            command.configuration().thresholds().accuracyMaxErrorPercent(),
            command.configuration().thresholds().timelinessDays(),
            command.configuration().thresholds().consistencyPercent()
        );
        
        thresholdRepository.save(threshold);
        
        logger.info("Successfully updated configuration for bankId: {}", command.bankId());
        
        // Return updated configuration via query handler
        return queryHandler.handle(new GetConfigurationQuery(command.bankId()));
    }
    
    private Result<Void> validateThresholds(ConfigurationDto.ThresholdsDto thresholds) {
        List<com.bcbs239.regtech.core.domain.shared.FieldError> fieldErrors = new ArrayList<>();
        
        // Validate completeness threshold
        Result<ThresholdPercentage> completeness = ThresholdPercentage.of(
            thresholds.completenessMinPercent(), "completeness"
        );
        if (completeness.isFailure()) {
            ErrorDetail error = completeness.getError().orElseThrow();
            fieldErrors.add(new com.bcbs239.regtech.core.domain.shared.FieldError(
                "thresholds.completenessMinPercent",
                error.getMessage(),
                error.getMessageKey()
            ));
        }
        
        // Validate accuracy threshold
        Result<ThresholdPercentage> accuracy = ThresholdPercentage.of(
            thresholds.accuracyMaxErrorPercent(), "accuracy"
        );
        if (accuracy.isFailure()) {
            ErrorDetail error = accuracy.getError().orElseThrow();
            fieldErrors.add(new com.bcbs239.regtech.core.domain.shared.FieldError(
                "thresholds.accuracyMaxErrorPercent",
                error.getMessage(),
                error.getMessageKey()
            ));
        }
        
        // Validate timeliness threshold
        Result<TimelinessDays> timeliness = TimelinessDays.of(
            thresholds.timelinessDays()
        );
        if (timeliness.isFailure()) {
            ErrorDetail error = timeliness.getError().orElseThrow();
            fieldErrors.add(new com.bcbs239.regtech.core.domain.shared.FieldError(
                "thresholds.timelinessDays",
                error.getMessage(),
                error.getMessageKey()
            ));
        }
        
        // Validate consistency threshold
        Result<ThresholdPercentage> consistency = ThresholdPercentage.of(
            thresholds.consistencyPercent(), "consistency"
        );
        if (consistency.isFailure()) {
            ErrorDetail error = consistency.getError().orElseThrow();
            fieldErrors.add(new com.bcbs239.regtech.core.domain.shared.FieldError(
                "thresholds.consistencyPercent",
                error.getMessage(),
                error.getMessageKey()
            ));
        }
        
        // If there are any validation errors, return them all
        if (!fieldErrors.isEmpty()) {
            return Result.failure(ErrorDetail.validationError(fieldErrors));
        }
        
        return Result.success(null);
    }
}
