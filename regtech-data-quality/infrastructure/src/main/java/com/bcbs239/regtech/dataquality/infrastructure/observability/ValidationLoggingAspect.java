package com.bcbs239.regtech.dataquality.infrastructure.observability;

import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;
import com.bcbs239.regtech.dataquality.application.validation.ValidationResults;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging validation execution.
 */
@Aspect
@Component
public class ValidationLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ValidationLoggingAspect.class);

    @Pointcut("execution(* com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionService.execute(..))")
    public void ruleExecution() {}

    @AfterReturning(pointcut = "ruleExecution()", returning = "results")
    public void logValidationSummary(ValidationResults results) {
        ValidationExecutionStats stats = results.stats();

        logger.debug("Rules Engine validation completed for exposure {}: " +
                "totalRules={}, executed={}, skipped={}, failed={}, " +
                "totalViolations={}, totalTime={}ms",
            results.exposureId(),
            results.stats().getExecuted().get() + results.stats().getSkipped().get() + results.stats().getFailed().get(),
            results.stats().getExecuted(),
            results.stats().getSkipped(),
            results.stats().getFailed(),
            results.getViolationCount(),
            results.stats().getTotalExecutionTimeMs()
        );
    }
}