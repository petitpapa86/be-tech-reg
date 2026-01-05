package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;
import com.bcbs239.regtech.dataquality.domain.validation.ExposureRecord;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationError;
import com.bcbs239.regtech.dataquality.domain.rules.BusinessRuleDto;
import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import com.bcbs239.regtech.dataquality.rulesengine.engine.RuleContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Application service for executing business rules.
 * Pure rule execution logic without persistence concerns.
 */
@Component
public class RuleExecutionService {

    private final RuleExecutionPort ruleExecutionPort;
    private final RuleContextFactory contextFactory;

    public RuleExecutionService(RuleExecutionPort ruleExecutionPort, RuleContextFactory contextFactory) {
        this.ruleExecutionPort = ruleExecutionPort;
        this.contextFactory = contextFactory;
    }

    /**
     * Executes all applicable rules for an exposure.
     *
     * @param exposure The exposure to validate
     * @param rules The list of business rules to execute
     * @return ValidationResults with all collected data
     */
    public ValidationResults execute(ExposureRecord exposure, List<BusinessRuleDto> rules) {
        RuleContext context = contextFactory.fromExposure(exposure);
        ValidationExecutionStats stats = new ValidationExecutionStats();

        List<ValidationError> errors = new ArrayList<>();
        List<RuleViolation> violations = new ArrayList<>();

        long start = System.currentTimeMillis();

        for (BusinessRuleDto rule : rules) {
            ruleExecutionPort.execute(
                rule, context, exposure,
                errors, violations, stats
            );
        }

        return new ValidationResults(
            exposure.exposureId(),
            errors,
            violations,
            stats
        );
    }
}