package com.bcbs239.regtech.dataquality.application.config;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import com.bcbs239.regtech.dataquality.domain.rules.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.domain.rules.BusinessRuleDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for managing data quality configuration.
 * 
 * <p>Orchestrates configuration operations by coordinating between:
 * <ul>
 *   <li>QualityThresholdRepository - for threshold persistence</li>
 *   <li>IBusinessRuleRepository - for validation rule management</li>
 *   <li>Domain models - QualityThreshold, BusinessRuleDto</li>
 * </ul>
 * 
 * <p><b>Top-Down Evolution:</b> Extracts business logic from controller,
 * providing a clean service layer that can be tested independently.
 * 
 * <p><b>Clean Architecture Layer:</b> Application (Use Cases)
 */
@Service
public class ConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);
    
    private final QualityThresholdRepository thresholdRepository;
    private final IBusinessRuleRepository businessRuleRepository;
    
    public ConfigurationService(
        QualityThresholdRepository thresholdRepository,
        IBusinessRuleRepository businessRuleRepository
    ) {
        this.thresholdRepository = thresholdRepository;
        this.businessRuleRepository = businessRuleRepository;
    }
    
    /**
     * Retrieves current configuration for a specific bank.
     * 
     * <p>Combines data from multiple sources:
     * <ul>
     *   <li>Quality thresholds from QualityThresholdRepository</li>
     *   <li>Active validation rules from IBusinessRuleRepository</li>
     *   <li>Error handling policies (currently hardcoded, will be configurable)</li>
     * </ul>
     * 
     * @param bankId Bank identifier
     * @return Configuration DTO or error if bank not found
     */
    public Result<ConfigurationDto> getConfiguration(String bankId) {
        logger.info("Fetching configuration for bankId: {}", bankId);
        
        try {
            // Fetch thresholds from repository
            QualityThreshold threshold = thresholdRepository.findByBankId(bankId)
                .orElseGet(() -> getDefaultThresholds(bankId));
            
            // Fetch active business rules
            List<BusinessRuleDto> activeRules = businessRuleRepository.findActiveRules();
            
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
            
            return Result.success(config);
            
        } catch (Exception e) {
            logger.error("Failed to fetch configuration for bankId: {}", bankId, e);
            return Result.failure(
                ErrorDetail.of(
                    "CONFIG_FETCH_FAILED",
                    ErrorType.SYSTEM_ERROR,
                    "Failed to retrieve configuration",
                    "config.fetch.failed"
                )
            );
        }
    }
    
    /**
     * Updates configuration for a specific bank.
     * 
     * <p>Persists changes to:
     * <ul>
     *   <li>Quality thresholds via QualityThresholdRepository</li>
     *   <li>Validation rules via IBusinessRuleRepository (future)</li>
     * </ul>
     * 
     * @param bankId Bank identifier
     * @param config Updated configuration
     * @return Updated configuration or error if validation fails
     */
    public Result<ConfigurationDto> updateConfiguration(String bankId, ConfigurationDto config) {
        logger.info("Updating configuration for bankId: {}", bankId);
        
        try {
            // Validate thresholds
            Result<Void> validation = validateThresholds(config.thresholds());
            if (validation.isFailure()) {
                return Result.failure(validation.getError());
            }
            
            // Persist thresholds
            QualityThreshold threshold = new QualityThreshold(
                bankId,
                config.thresholds().completenessMinPercent(),
                config.thresholds().accuracyMaxErrorPercent(),
                config.thresholds().timelinessDays(),
                config.thresholds().consistencyPercent()
            );
            
            thresholdRepository.save(threshold);
            
            logger.info("Successfully updated configuration for bankId: {}", bankId);
            
            // Return updated configuration
            return getConfiguration(bankId);
            
        } catch (Exception e) {
            logger.error("Failed to update configuration for bankId: {}", bankId, e);
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
    
    /**
     * Resets configuration to system defaults for a specific bank.
     * 
     * @param bankId Bank identifier
     * @return Default configuration
     */
    public Result<ConfigurationDto> resetToDefaults(String bankId) {
        logger.info("Resetting configuration to defaults for bankId: {}", bankId);
        
        try {
            QualityThreshold defaultThreshold = getDefaultThresholds(bankId);
            thresholdRepository.save(defaultThreshold);
            
            return getConfiguration(bankId);
            
        } catch (Exception e) {
            logger.error("Failed to reset configuration for bankId: {}", bankId, e);
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
    
    /**
     * Validates threshold values.
     */
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
    
    /**
     * Returns default thresholds for new banks.
     */
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
