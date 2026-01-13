package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import com.bcbs239.regtech.dataquality.domain.config.BankId;
import com.bcbs239.regtech.dataquality.domain.config.CompletenessThreshold;
import com.bcbs239.regtech.dataquality.domain.config.AccuracyThreshold;
import com.bcbs239.regtech.dataquality.domain.config.TimelinessThreshold;
import com.bcbs239.regtech.dataquality.domain.config.ConsistencyThreshold;
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
    
    // Default threshold constants using value objects for type safety
    private static final CompletenessThreshold DEFAULT_COMPLETENESS = 
        CompletenessThreshold.of(0.95).getValueOrThrow();
    private static final AccuracyThreshold DEFAULT_ACCURACY = 
        AccuracyThreshold.of(0.05).getValueOrThrow();
    private static final TimelinessThreshold DEFAULT_TIMELINESS = 
        TimelinessThreshold.of(7).getValueOrThrow();
    private static final ConsistencyThreshold DEFAULT_CONSISTENCY = 
        ConsistencyThreshold.of(0.98).getValueOrThrow();
    
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
        logger.info("Handling ResetConfigurationCommand for bankId: {}", command.bankId().value());
        
        QualityThreshold defaultThreshold = getDefaultThresholds(command.bankId());
        thresholdRepository.save(defaultThreshold);
        
        logger.info("Successfully reset configuration for bankId: {}", command.bankId().value());
        
        // Return configuration via query handler
        return queryHandler.handle(new GetConfigurationQuery(command.bankId()));
    }
    
    private QualityThreshold getDefaultThresholds(BankId bankId) {
        return new QualityThreshold(
            bankId.value(),
            DEFAULT_COMPLETENESS.value(),
            DEFAULT_ACCURACY.value(),
            DEFAULT_TIMELINESS.value(),
            DEFAULT_CONSISTENCY.value()
        );
    }
}
