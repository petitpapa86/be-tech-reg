package com.bcbs239.regtech.reportgeneration.infrastructure.observability;

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
 * Aspect that logs entry and exit of public methods in the report-generation application layer.
 * Follows the project's logging guideline: lightweight entry/exit, execution time, and error logging.
 */
@Aspect
@Component("reportGenerationUseCaseLoggingAspect")
public class UseCaseLoggingAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(UseCaseLoggingAspect.class);

    // Pointcut matching public methods in the reportgeneration application package
    @Pointcut("execution(public * com.bcbs239.regtech.reportgeneration.application..*(..))")
    public void useCaseMethods() {
    }

    @Around("useCaseMethods()")
    public Object logUseCaseExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String fullMethodName = className + "." + methodName;

        Object[] args = joinPoint.getArgs();
        String argsDescription = formatArguments(args);

        Instant start = Instant.now();

        String prevUseCaseClass = MDC.get("use-case-class");
        String prevUseCaseMethod = MDC.get("use-case-method");

        try {
            MDC.put("use-case-class", className);
            MDC.put("use-case-method", methodName);

            if (log.isDebugEnabled()) {
                log.debug("⮕ USE-CASE START: {} args={}", fullMethodName, argsDescription);
            } else {
                log.info("⮕ USE-CASE START: {}", fullMethodName);
            }

            Object result = joinPoint.proceed();

            Duration duration = Duration.between(start, Instant.now());

            if (log.isDebugEnabled()) {
                log.debug("⮐ USE-CASE SUCCESS: {} duration={}ms result={}",
                        fullMethodName,
                        duration.toMillis(),
                        result == null ? "void/null" : result.getClass().getSimpleName());
            } else {
                log.info("⮐ USE-CASE SUCCESS: {} duration={}ms", fullMethodName, duration.toMillis());
            }

            return result;

        } catch (Throwable t) {
            Duration duration = Duration.between(start, Instant.now());
            log.error("⮐ USE-CASE FAILED: {} duration={}ms error={}",
                    fullMethodName,
                    duration.toMillis(),
                    t.getClass().getSimpleName(),
                    t);
            throw t;
        } finally {
            if (prevUseCaseClass != null) {
                MDC.put("use-case-class", prevUseCaseClass);
            } else {
                MDC.remove("use-case-class");
            }
            if (prevUseCaseMethod != null) {
                MDC.put("use-case-method", prevUseCaseMethod);
            } else {
                MDC.remove("use-case-method");
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // run late to capture context
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) return "null";
                    if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) return arg.toString();
                    return arg.getClass().getSimpleName();
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
