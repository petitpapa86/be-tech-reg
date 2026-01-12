package com.bcbs239.regtech.dataquality.application.config;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for data quality configuration.
 * 
 * <p>Represents the complete configuration including thresholds, validation rules,
 * error handling policies, and audit metadata. This DTO serves as the contract
 * between presentation and application layers.
 * 
 * <p><b>Top-Down Evolution:</b> Extracted from Map<String, Object> in controller
 * to provide type safety and validation.
 */
public record ConfigurationDto(
    ThresholdsDto thresholds,
    ValidationDto validation,
    ErrorHandlingDto errorHandling,
    ConfigurationStatusDto status
) {
    
    /**
     * Quality thresholds configuration.
     */
    public record ThresholdsDto(
        int completenessMinPercent,
        double accuracyMaxErrorPercent,
        int timelinessDays,
        double consistencyPercent
    ) {}
    
    /**
     * Validation configuration with active rules.
     */
    public record ValidationDto(
        String type,
        List<ValidationRuleDto> activeRules
    ) {}
    
    /**
     * Individual validation rule definition.
     */
    public record ValidationRuleDto(
        String code,
        String description,
        boolean enabled
    ) {}
    
    /**
     * Error handling policies.
     */
    public record ErrorHandlingDto(
        String criticalErrors,
        String warnings,
        boolean emailNotificationEnabled
    ) {}
    
    /**
     * Configuration metadata and audit trail.
     */
    public record ConfigurationStatusDto(
        boolean valid,
        int activeValidationRules,
        LocalDateTime lastModifiedAt,
        String lastModifiedBy
    ) {}
}
