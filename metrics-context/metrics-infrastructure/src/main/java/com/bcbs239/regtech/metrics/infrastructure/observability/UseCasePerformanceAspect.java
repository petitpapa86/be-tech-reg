package com.bcbs239.regtech.metrics.infrastructure.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Performance monitoring aspect for use cases.
 * Tracks execution time and can alert on slow operations.
 * This is optional and can be enabled via configuration.
 */
@Aspect
@Component
@ConditionalOnProperty(name = "metrics.observability.performance-monitoring.enabled", havingValue = "true", matchIfMissing = true)
public class UseCasePerformanceAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(UseCasePerformanceAspect.class);

    // Threshold for warning about slow use cases (in milliseconds)
    private static final long SLOW_EXECUTION_THRESHOLD_MS = 5000;

    /**
     * Pointcut matching all public methods in classes ending with "UseCase" 
     * in the metrics application layer.
     */
    @Pointcut("execution(public * com.bcbs239.regtech.metrics.application..*UseCase.*(..))")
    public void useCaseMethods() {
    }

    /**
     * Around advice that monitors performance and warns about slow operations.
     */
    @Around("useCaseMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringType().getSimpleName() + 
                           "." + joinPoint.getSignature().getName();

        Instant startTime = Instant.now();

        try {
            Object result = joinPoint.proceed();

            Duration executionTime = Duration.between(startTime, Instant.now());
            long executionMs = executionTime.toMillis();

            // Warn about slow operations
            if (executionMs > SLOW_EXECUTION_THRESHOLD_MS) {
                log.warn("SLOW USE CASE: {} took {} ms (threshold: {} ms)",
                        methodName,
                        executionMs,
                        SLOW_EXECUTION_THRESHOLD_MS);
            }

            return result;

        } catch (Throwable throwable) {
            // Still measure time even on failure
            Duration executionTime = Duration.between(startTime, Instant.now());
            long executionMs = executionTime.toMillis();

            if (executionMs > SLOW_EXECUTION_THRESHOLD_MS) {
                log.warn("SLOW USE CASE (with failure): {} took {} ms before failing",
                        methodName,
                        executionMs);
            }

            throw throwable;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1; // Execute after MDC enrichment
    }
}
