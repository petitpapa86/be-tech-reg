package com.bcbs239.regtech.metrics.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Metrics collection aspect for use cases.
 * Records execution time, success/failure rates, and other operational metrics.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "metrics.observability.metrics-collection.enabled", havingValue = "true", matchIfMissing = true)
public class UseCaseMetricsAspect implements Ordered {

    private final MeterRegistry meterRegistry;

    public UseCaseMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Pointcut matching all public methods in classes ending with "UseCase" 
     * in the metrics application layer.
     */
    @Pointcut("execution(public * com.bcbs239.regtech.metrics.application..*UseCase.*(..))")
    public void useCaseMethods() {
    }

    /**
     * Around advice that records metrics for use case execution.
     */
    @Around("useCaseMethods()")
    public Object recordMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String useCaseName = className + "." + methodName;

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();

            // Record successful execution
            sample.stop(Timer.builder("metrics.usecase.execution.duration")
                    .tag("usecase", useCaseName)
                    .tag("status", "success")
                    .description("Use case execution duration")
                    .register(meterRegistry));

            meterRegistry.counter("metrics.usecase.execution.total",
                    "usecase", useCaseName,
                    "status", "success"
            ).increment();

            return result;

        } catch (Exception e) {
            // Record failed execution
            sample.stop(Timer.builder("metrics.usecase.execution.duration")
                    .tag("usecase", useCaseName)
                    .tag("status", "error")
                    .tag("error_type", e.getClass().getSimpleName())
                    .description("Use case execution duration")
                    .register(meterRegistry));

            meterRegistry.counter("metrics.usecase.execution.total",
                    "usecase", useCaseName,
                    "status", "error",
                    "error_type", e.getClass().getSimpleName()
            ).increment();

            throw e;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2; // Execute after performance monitoring
    }
}
