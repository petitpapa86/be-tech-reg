package com.bcbs239.regtech.dataquality.infrastructure.observability;

import com.bcbs239.regtech.dataquality.application.validation.ValidationExecutionStats;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect for metrics collection on validation execution.
 */
@Aspect
@Component
public class ValidationMetricsAspect {

    private final MeterRegistry meterRegistry;

    public ValidationMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Pointcut("execution(* com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionService.execute(..))")
    public void ruleExecution() {}

    @Around("ruleExecution()")
    @Timed(value = "dataquality.validation.rule.execution", description = "Time taken to execute rules for an exposure")
    public Object collectMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            sample.stop(Timer.builder("dataquality.validation.rule.execution")
                .description("Time taken to execute rules for an exposure")
                .register(meterRegistry));

            return result;
        } catch (Exception e) {
            sample.stop(Timer.builder("dataquality.validation.rule.execution.error")
                .description("Time taken for failed rule execution")
                .register(meterRegistry));
            throw e;
        }
    }
}