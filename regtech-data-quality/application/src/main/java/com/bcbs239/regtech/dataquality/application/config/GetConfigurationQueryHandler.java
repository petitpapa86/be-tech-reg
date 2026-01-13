package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import com.bcbs239.regtech.dataquality.domain.rules.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.domain.rules.BusinessRuleDto;
import com.bcbs239.regtech.dataquality.domain.config.BankId;
import com.bcbs239.regtech.dataquality.domain.config.CompletenessThreshold;
import com.bcbs239.regtech.dataquality.domain.config.AccuracyThreshold;
import com.bcbs239.regtech.dataquality.domain.config.TimelinessThreshold;
import com.bcbs239.regtech.dataquality.domain.config.ConsistencyThreshold;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query handler for retrieving data quality configuration.
 * 
 * <p>CQRS Pattern: Handles READ operations for configuration.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch quality thresholds from QualityThresholdRepository</li>
 *   <li>Fetch active validation rules from IBusinessRuleRepository</li>
 *   <li>Combine data into ConfigurationDto</li>
 * </ul>
 */
@Component
public class GetConfigurationQueryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GetConfigurationQueryHandler.class);
    
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
    private final IBusinessRuleRepository businessRuleRepository;
    
    public GetConfigurationQueryHandler(
        QualityThresholdRepository thresholdRepository,
        IBusinessRuleRepository businessRuleRepository
    ) {
        this.thresholdRepository = thresholdRepository;
        this.businessRuleRepository = businessRuleRepository;
    }
    
    @Observed(name = "config.query.get", contextualName = "Get Configuration Query")
    public Result<ConfigurationDto> handle(GetConfigurationQuery query) {
        logger.info("Handling GetConfigurationQuery for bankId: {}", query.bankId().value());
        
        // Fetch thresholds from repository
        QualityThreshold threshold = thresholdRepository.findByBankId(query.bankId().value())
            .orElseGet(() -> getDefaultThresholds(query.bankId().value()));
        
        // Fetch active business rules (using current date)
        List<BusinessRuleDto> activeRules = businessRuleRepository.findActiveRules(java.time.LocalDate.now());
        
        // Build configuration DTO
        ConfigurationDto config = new ConfigurationDto(
            new ConfigurationDto.ThresholdsDto(
                (int) threshold.completenessMinPercent(),
                threshold.accuracyMaxErrorPercent(),
                threshold.timelinessDays(),
                threshold.consistencyPercent()
            ),
            new ConfigurationDto.ValidationDto(
                "AUTOMATIC",
                activeRules.stream()
                    .map(rule -> new ConfigurationDto.ValidationRuleDto(
                        rule.ruleCode(),
                        rule.description(),
                        rule.enabled()
                    ))
                    .collect(Collectors.toList())
            ),
            new ConfigurationDto.ErrorHandlingDto(
                "REJECT_FILE",
                "MANUAL_REVIEW",
                true
            ),
            new ConfigurationDto.ConfigurationStatusDto(
                true,
                (int) activeRules.stream().filter(BusinessRuleDto::enabled).count(),
                LocalDateTime.now(),
                "System"
            )
        );
        
        logger.info("Successfully retrieved configuration for bankId: {}", query.bankId());
        return Result.success(config);
    }
    
    private QualityThreshold getDefaultThresholds(String bankId) {
        return new QualityThreshold(
            bankId,
            DEFAULT_COMPLETENESS.value(),
            DEFAULT_ACCURACY.value(),
            DEFAULT_TIMELINESS.value(),
            DEFAULT_CONSISTENCY.value()
        );
    }
}
