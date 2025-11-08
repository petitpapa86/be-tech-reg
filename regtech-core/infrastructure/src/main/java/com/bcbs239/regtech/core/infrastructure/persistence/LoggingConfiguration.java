package com.bcbs239.regtech.core.infrastructure.persistence;

import com.bcbs239.regtech.core.infrastructure.systemservices.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Enhanced logging configuration for GCP deployment with structured JSON logging.
 * Provides correlation ID tracking, event processing logging, and GCP-compatible log formats.
 * Uses ScopedValue for thread-safe context propagation with virtual threads.
 */
@Configuration
public class LoggingConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

    private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // Scoped Values for logging context
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> SERVICE = ScopedValue.newInstance();
    public static final ScopedValue<String> EVENT_TYPE = ScopedValue.newInstance();
    public static final ScopedValue<String> VERSION = ScopedValue.newInstance();
    public static final ScopedValue<Map<String, Object>> LOGGING_DETAILS = ScopedValue.newInstance();
    public static final ScopedValue<Long> REQUEST_START_TIME = ScopedValue.newInstance();

    @Bean
    public ObjectMapper loggingObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CorrelationIdInterceptor());
    }

    /**
     * Gracefully shutdown the executor on application shutdown
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down virtual thread executor for logging");
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Executor did not terminate in time, forcing shutdown");
                virtualThreadExecutor.shutdownNow();
                if (!virtualThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("Executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for executor shutdown", e);
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static class CorrelationIdInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String correlationId = request.getHeader("X-Correlation-ID");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = CorrelationId.generate().id();
            }
            
            final String finalCorrelationId = correlationId;
            final long startTime = System.currentTimeMillis();
            
            // Bind ScopedValues for the request scope
            ScopedValue.where(CORRELATION_ID, finalCorrelationId)
                      .where(SERVICE, "regtech")
                      .where(VERSION, "1.0.0")
                      .where(REQUEST_START_TIME, startTime)
                      .run(() -> {
                // Also set MDC for compatibility with synchronous logging
                MDC.put("correlationId", finalCorrelationId);
                MDC.put("requestMethod", request.getMethod());
                MDC.put("requestUri", request.getRequestURI());
                MDC.put("userAgent", request.getHeader("User-Agent"));
            });
            
            response.setHeader("X-Correlation-ID", correlationId);

            Map<String, Object> details = new HashMap<>();
            details.put("method", request.getMethod());
            details.put("uri", request.getRequestURI());
            details.put("correlationId", correlationId);
            
            logger.info("Request started: {} {}", request.getMethod(), request.getRequestURI());
            logStructured("Request started", details);

            // Store values in request attributes for afterCompletion
            request.setAttribute("correlationId", correlationId);
            request.setAttribute("startTime", startTime);

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            String correlationId = (String) request.getAttribute("correlationId");
            Long startTime = (Long) request.getAttribute("startTime");
            long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
            int statusCode = response.getStatus();

            Map<String, Object> details = new HashMap<>();
            details.put("correlationId", correlationId);
            details.put("statusCode", statusCode);
            details.put("duration", duration);
            details.put("method", request.getMethod());
            details.put("uri", request.getRequestURI());

            if (ex != null) {
                details.put("error", ex.getMessage());
                details.put("exceptionClass", ex.getClass().getName());
                logger.error("Request failed: {} {} - Status: {} - Duration: {}ms", 
                    request.getMethod(), request.getRequestURI(), statusCode, duration, ex);
                logStructured("Request failed", details, ex);
            } else {
                logger.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                    request.getMethod(), request.getRequestURI(), statusCode, duration);
                logStructured("Request completed", details);
            }

            // Clean up MDC
            MDC.remove("correlationId");
            MDC.remove("requestMethod");
            MDC.remove("requestUri");
            MDC.remove("userAgent");
            MDC.clear();
        }
    }

    /**
     * Get the current correlation ID from ScopedValue or MDC fallback
     */
    public static String getCurrentCorrelationId() {
        String correlationId = null;
        if (CORRELATION_ID.isBound()) {
            correlationId = CORRELATION_ID.get();
        }
        if (correlationId == null) {
            correlationId = MDC.get("correlationId");
        }
        return correlationId;
    }

    /**
     * Create a structured log entry for GCP
     */
    public static Map<String, Object> createStructuredLog(String eventType, Map<String, Object> details) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("eventType", eventType);
        logEntry.put("correlationId", getCurrentCorrelationId());
        logEntry.put("service", SERVICE.isBound() ? SERVICE.get() : "regtech");
        logEntry.put("version", VERSION.isBound() ? VERSION.get() : "1.0.0");

        // Capture line number
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int lineNumber = 0;
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (!className.startsWith("com.bcbs239.regtech.core.config.LoggingConfiguration") &&
                !className.startsWith("java.") &&
                !className.startsWith("jdk.")) {
                lineNumber = element.getLineNumber();
                break;
            }
        }
        logEntry.put("lineNumber", lineNumber);

        if (details != null) {
            logEntry.putAll(details);
        }

        return logEntry;
    }

    /**
     * Log event processing with structured format
     */
    public static void logEventProcessing(String operation, String eventId, String eventType,
                                        String status, Map<String, Object> additionalDetails) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", operation);
        details.put("eventId", eventId);
        details.put("eventType", eventType);
        details.put("status", status);

        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        Logger eventLogger = LoggerFactory.getLogger("com.bcbs239.regtech.events");
        eventLogger.info("Event processed: {} - {} - {}", operation, eventId, status);
        logStructured("Event processed", details);
    }

    /**
     * Log batch processing metrics
     */
    public static void logBatchProcessing(String processorType, String context, int batchSize,
                                        int processed, int failed, long duration) {
        Map<String, Object> details = new HashMap<>();
        details.put("processorType", processorType);
        details.put("context", context);
        details.put("batchSize", batchSize);
        details.put("processed", processed);
        details.put("failed", failed);
        details.put("duration", duration);
        details.put("successRate", batchSize > 0 ? (double) processed / batchSize : 0.0);

        Logger batchLogger = LoggerFactory.getLogger("com.bcbs239.regtech.batch");
        batchLogger.info("Batch processed: {} - {}/{} succeeded in {}ms", 
            processorType, processed, batchSize, duration);
        logStructured("Batch processed", details);
    }

    /**
     * Log errors with full context
     */
    public static void logError(String message, Throwable throwable,
                              Map<String, Object> context) {
        Map<String, Object> details = new HashMap<>();

        details.put("message", message);

        if (context != null) {
            details.putAll(context);
        }

        Logger errorLogger = LoggerFactory.getLogger("com.bcbs239.regtech.errors");
        errorLogger.error("Error occurred: {} ", message, throwable);
        logStructured("Error occurred", details, throwable);
    }

    /**
     * Get stack trace as string for logging
     */
    private static String getStackTraceAsString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Combined structured logging method that integrates SLF4J logging with structured data.
     * Logs the message using SLF4J and includes structured details as JSON.
     * Uses virtual threads for non-blocking async logging with proper error handling.
     */
    public static void logStructured(String message, Map<String, Object> details) {
        logStructured(message, details, null);
    }

    /**
     * Combined structured logging method that integrates SLF4J logging with structured data.
     * Logs the message using SLF4J and includes structured details as JSON.
     * Uses virtual threads for non-blocking async logging with proper error handling.
     * 
     * @param message The log message
     * @param details Additional structured details to log
     * @param throwable Optional exception to log
     */
    public static void logStructured(String message, Map<String, Object> details, Throwable throwable) {
        // Capture calling class, service, and line number before async execution
        final String loggerName;
        final String service;
        final int lineNumber;
        final String eventType;
        final Map<String, Object> capturedDetails;
        
        {
            String tempLoggerName = "com.bcbs239.regtech.structured";
            String tempService = SERVICE.isBound() ? SERVICE.get() : "regtech";
            int tempLineNumber = 0;
            String tempEventType = details != null && details.containsKey("eventType") 
                ? String.valueOf(details.get("eventType")) : "LOG";
            Map<String, Object> tempDetails = new HashMap<>();
            if (details != null) {
                tempDetails.putAll(details);
            }

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("com.bcbs239.regtech.core.config.LoggingConfiguration") &&
                    !className.startsWith("java.") &&
                    !className.startsWith("jdk.")) {
                    tempLoggerName = className;
                    tempLineNumber = element.getLineNumber();
                    String[] parts = className.split("\\.");
                    if (parts.length >= 4 && "com.bcbs239.regtech".equals(parts[0] + "." + parts[1] + "." + parts[2])) {
                        tempService = "regtech-" + parts[3];
                    }
                    break;
                }
            }
            loggerName = tempLoggerName;
            service = tempService;
            lineNumber = tempLineNumber;
            eventType = tempEventType;
            capturedDetails = tempDetails;
        }

        // Capture current scoped values for propagation (safely check if bound)
        final String correlationId = CORRELATION_ID.isBound() ? CORRELATION_ID.get() : MDC.get("correlationId");
        final String version = VERSION.isBound() ? VERSION.get() : "1.0.0";

        // Submit to virtual thread executor with error handling
        try {
            virtualThreadExecutor.submit(() -> {
                try {
                    // Bind scoped values in the virtual thread
                    ScopedValue.where(CORRELATION_ID, correlationId)
                              .where(SERVICE, service)
                              .where(EVENT_TYPE, eventType)
                              .where(VERSION, version)
                              .where(LOGGING_DETAILS, capturedDetails)
                              .run(() -> {
                        try {
                            Logger structuredLogger = LoggerFactory.getLogger(loggerName);

                            // Populate MDC from Scoped Values for SLF4J compatibility
                            if (correlationId != null) {
                                MDC.put("correlationId", correlationId);
                            }
                            MDC.put("service", service);
                            MDC.put("eventType", eventType);
                            MDC.put("lineNumber", String.valueOf(lineNumber));
                            MDC.put("version", version);
                            
                            if (!capturedDetails.isEmpty()) {
                                capturedDetails.forEach((key, value) -> 
                                    MDC.put(key, value != null ? value.toString() : "null"));
                            }

                            if (throwable != null) {
                                MDC.put("error", throwable.getMessage());
                                MDC.put("exceptionClass", throwable.getClass().getName());
                                structuredLogger.error(message, throwable);
                            } else {
                                structuredLogger.info(message);
                            }
                        } finally {
                            // Clear MDC to prevent leaks
                            MDC.clear();
                        }
                    });
                } catch (Exception e) {
                    // Fallback logging if structured logging fails
                    Logger fallbackLogger = LoggerFactory.getLogger(LoggingConfiguration.class);
                    fallbackLogger.error("Failed to log structured message: {} - Error: {}", 
                        message, e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            // If executor submission fails, log synchronously as fallback
            Logger fallbackLogger = LoggerFactory.getLogger(LoggingConfiguration.class);
            fallbackLogger.error("Failed to submit log task to executor: {} - Error: {}", 
                message, e.getMessage(), e);
            
            // Attempt synchronous logging
            try {
                Logger structuredLogger = LoggerFactory.getLogger(loggerName);
                if (throwable != null) {
                    structuredLogger.error(message, throwable);
                } else {
                    structuredLogger.info(message);
                }
            } catch (Exception syncError) {
                fallbackLogger.error("Synchronous logging also failed", syncError);
            }
        }
    }

    /**
     * Execute code with a specific correlation ID in scope
     * Useful for background tasks or async operations
     */
    public static void withCorrelationId(String correlationId, Runnable task) {
        ScopedValue.where(CORRELATION_ID, correlationId)
                  .where(SERVICE, "regtech")
                  .where(VERSION, "1.0.0")
                  .run(() -> {
            MDC.put("correlationId", correlationId);
            try {
                task.run();
            } finally {
                MDC.remove("correlationId");
            }
        });
    }

    /**
     * Execute code with full logging context
     */
    public static <T> T withLoggingContext(String correlationId, String service, 
                                          Map<String, Object> details, 
                                          java.util.function.Supplier<T> task) {
        return ScopedValue.where(CORRELATION_ID, correlationId)
                         .where(SERVICE, service)
                         .where(VERSION, "1.0.0")
                         .where(LOGGING_DETAILS, details)
                         .call(() -> {
            MDC.put("correlationId", correlationId);
            MDC.put("service", service);
            try {
                return task.get();
            } finally {
                MDC.clear();
            }
        });
    }
}

