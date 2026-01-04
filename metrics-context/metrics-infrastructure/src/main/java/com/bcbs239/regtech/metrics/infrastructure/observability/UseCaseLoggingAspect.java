package com.bcbs239.regtech.metrics.infrastructure.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Aspect that logs entry and exit of all use case methods in the metrics application layer.
 * This removes the need for explicit logging in the application layer.
 * Executes last in the aspect chain to capture all context.
 */
@Aspect
@Component
public class UseCaseLoggingAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(UseCaseLoggingAspect.class);

    /**
     * Pointcut matching all public methods in classes ending with "UseCase" 
     * in the metrics application layer.
     */
    @Pointcut("execution(public * com.bcbs239.regtech.metrics.application..*UseCase.*(..))")
    public void useCaseMethods() {
    }

    /**
     * Around advice that logs method entry, exit, and execution time.
     * Also handles exceptions and logs them appropriately.
     */
    @Around("useCaseMethods()")
    public Object logUseCaseExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String fullMethodName = className + "." + methodName;

        // Extract parameters for logging (be careful with sensitive data)
        Object[] args = joinPoint.getArgs();
        String argsDescription = formatArguments(args);

        Instant startTime = Instant.now();

        // Note: Do NOT use try-with-resources for MDC here
        // MDC is already set by MdcEnrichmentAspect and should persist
        String previousUseCaseClass = MDC.get("use-case-class");
        String previousUseCaseMethod = MDC.get("use-case-method");
        
        try {
            // Add use-case specific MDC
            MDC.put("use-case-class", className);
            MDC.put("use-case-method", methodName);
            
            // Log entry
            if (log.isDebugEnabled()) {
                log.debug("⮕ USE-CASE START: {} args={}", fullMethodName, argsDescription);
            } else {
                log.info("⮕ USE-CASE START: {}", fullMethodName);
            }

            // Execute the method
            Object result = joinPoint.proceed();

            // Calculate execution time
            Duration executionTime = Duration.between(startTime, Instant.now());

            // Log successful exit
            if (log.isDebugEnabled()) {
                log.debug("⮐ USE-CASE SUCCESS: {} duration={}ms result={}",
                        fullMethodName,
                        executionTime.toMillis(),
                        result == null ? "void/null" : result.getClass().getSimpleName());
            } else {
                log.info("⮐ USE-CASE SUCCESS: {} duration={}ms",
                        fullMethodName,
                        executionTime.toMillis());
            }

            return result;

        } catch (Throwable throwable) {
            // Calculate execution time
            Duration executionTime = Duration.between(startTime, Instant.now());

            // Log exception
            log.error("⮐ USE-CASE FAILED: {} duration={}ms error={}",
                    fullMethodName,
                    executionTime.toMillis(),
                    throwable.getClass().getSimpleName(),
                    throwable);

            // Re-throw the exception
            throw throwable;
        } finally {
            // Restore previous MDC values
            if (previousUseCaseClass != null) {
                MDC.put("use-case-class", previousUseCaseClass);
            } else {
                MDC.remove("use-case-class");
            }
            if (previousUseCaseMethod != null) {
                MDC.put("use-case-method", previousUseCaseMethod);
            } else {
                MDC.remove("use-case-method");
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Execute last to capture complete context
    }

    /**
     * Formats method arguments for logging.
     * Avoid logging sensitive data like passwords or tokens.
     */
    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    // For simple types, log the value
                    if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                        return arg.toString();
                    }
                    // For complex types, just log the type name
                    return arg.getClass().getSimpleName();
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
