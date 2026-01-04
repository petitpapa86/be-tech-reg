package com.bcbs239.regtech.metrics.infrastructure.observability;

import com.bcbs239.regtech.metrics.domain.BankId;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * Aspect that enriches MDC (Mapped Diagnostic Context) with business identifiers
 * extracted from use case method parameters.
 * 
 * <p>This allows logs to automatically include contextual information like bankId
 * without requiring explicit MDC.put() calls in the application layer.</p>
 * 
 * <p>Uses Ordered.HIGHEST_PRECEDENCE to execute first and maintain MDC context
 * throughout the entire aspect chain.</p>
 */
@Aspect
@Component
public class MdcEnrichmentAspect implements Ordered {

    /**
     * Pointcut matching all public methods in classes ending with "UseCase" 
     * in the metrics application layer.
     */
    @Pointcut("execution(public * com.bcbs239.regtech.metrics.application..*UseCase.*(..))")
    public void useCaseMethods() {
    }

    /**
     * Around advice that extracts business identifiers from parameters
     * and adds them to MDC for the duration of the use case execution.
     * 
     * <p>MDC is properly cleaned up after all nested aspects complete.</p>
     */
    @Around("useCaseMethods()")
    public Object enrichMdc(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        
        // Extract BankId if present in parameters
        String bankId = extractBankId(args);
        
        // Store previous values to restore later
        String previousBankId = MDC.get("bank-id");
        
        try {
            // Set MDC values
            if (bankId != null) {
                MDC.put("bank-id", bankId);
            }
            
            // Proceed with the call - MDC will be available to all nested aspects
            return joinPoint.proceed();
            
        } finally {
            // Restore previous values
            if (previousBankId != null) {
                MDC.put("bank-id", previousBankId);
            } else if (bankId != null) {
                MDC.remove("bank-id");
            }
        }
    }

    /**
     * Extracts bank ID from method arguments if present.
     */
    private String extractBankId(Object[] args) {
        if (args == null) {
            return null;
        }
        
        for (Object arg : args) {
            if (arg instanceof BankId bankIdObj) {
                return bankIdObj.getValue();
            }
            // Could extract from other domain objects if needed
            // e.g., if (arg instanceof DomainEvent event) { return event.bankId(); }
        }
        
        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Execute first, before all other aspects
    }
}
