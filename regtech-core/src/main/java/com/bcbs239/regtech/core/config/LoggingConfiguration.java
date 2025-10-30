package com.bcbs239.regtech.core.config;

import com.bcbs239.regtech.core.shared.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced logging configuration for GCP deployment with structured JSON logging.
 * Provides correlation ID tracking, event processing logging, and GCP-compatible log formats.
 */
@Configuration
public class LoggingConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

    private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

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

    private static class CorrelationIdInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String correlationId = request.getHeader("X-Correlation-ID");
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = CorrelationId.generate().getId();
            }
            MDC.put("correlationId", correlationId);
            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestUri", request.getRequestURI());
            MDC.put("userAgent", request.getHeader("User-Agent"));
            response.setHeader("X-Correlation-ID", correlationId);

            logger.info("Request started", createStructuredLog("REQUEST_STARTED", Map.of(
                "method", request.getMethod(),
                "uri", request.getRequestURI(),
                "correlationId", correlationId
            )));

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            String correlationId = MDC.get("correlationId");
            int statusCode = response.getStatus();

            if (ex != null) {
                logger.error("Request failed", createStructuredLog("REQUEST_FAILED", Map.of(
                    "correlationId", correlationId,
                    "statusCode", statusCode,
                    "error", ex.getMessage()
                )), ex);
            } else {
                logger.info("Request completed", createStructuredLog("REQUEST_COMPLETED", Map.of(
                    "correlationId", correlationId,
                    "statusCode", statusCode,
                    "duration", System.currentTimeMillis() - getRequestStartTime()
                )));
            }

            // Clean up MDC
            MDC.remove("correlationId");
            MDC.remove("requestMethod");
            MDC.remove("requestUri");
            MDC.remove("userAgent");
        }

        private long getRequestStartTime() {
            // This would need to be set in preHandle and retrieved here
            // For now, return current time as approximation
            return System.currentTimeMillis();
        }
    }

    /**
     * Get the current correlation ID from MDC
     */
    public static String getCurrentCorrelationId() {
        return MDC.get("correlationId");
    }

    /**
     * Create a structured log entry for GCP
     */
    public static Map<String, Object> createStructuredLog(String eventType, Map<String, Object> details) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("eventType", eventType);
        logEntry.put("correlationId", MDC.get("correlationId"));
        logEntry.put("service", "regtech");
        logEntry.put("version", "1.0.0");

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
        details.put("module", MDC.get("module"));

        if (additionalDetails != null) {
            details.putAll(additionalDetails);
        }

        Logger eventLogger = LoggerFactory.getLogger("com.bcbs239.regtech.events");
        eventLogger.info("Event processed", createStructuredLog("EVENT_" + operation.toUpperCase(), details));
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
        batchLogger.info("Batch processed", createStructuredLog("BATCH_" + processorType.toUpperCase() + "_COMPLETED", details));
    }

    /**
     * Log errors with full context
     */
    public static void logError(String operation, String errorType, String message, Throwable throwable,
                              Map<String, Object> context) {
        Map<String, Object> details = new HashMap<>();
        details.put("operation", operation);
        details.put("errorType", errorType);
        details.put("message", message);

        if (context != null) {
            details.putAll(context);
        }

        Logger errorLogger = LoggerFactory.getLogger("com.bcbs239.regtech.errors");
        errorLogger.error("Error occurred", createStructuredLog("ERROR_" + operation.toUpperCase(), details), throwable);
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
     * Includes thread information and supports exception logging for async contexts.
     * Uses virtual threads for non-blocking async logging.
     */
    public static void logStructured(String message, Map<String, Object> details) {
        logStructured(message, details, null);
    }

    /**
     * Combined structured logging method that integrates SLF4J logging with structured data.
     * Logs the message using SLF4J and includes structured details as JSON.
     * Includes thread information and supports exception logging for async contexts.
     * Uses virtual threads for non-blocking async logging.
     */
    public static void logStructured(String message, Map<String, Object> details, Throwable throwable) {
        // Capture calling class, service, and line number before async execution
        final String loggerName;
        final String service;
        final String lineNumber;
        final String eventType;
        {
            String tempLoggerName = "com.bcbs239.regtech.structured";
            String tempService = "regtech";
            String tempLineNumber = "0";
            String tempEventType = details != null && details.containsKey("eventType") ? String.valueOf(details.get("eventType")) : "LOG";
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (!className.startsWith("com.bcbs239.regtech.core.config.LoggingConfiguration") &&
                    !className.startsWith("java.") &&
                    !className.startsWith("jdk.")) {
                    tempLoggerName = className;
                    tempLineNumber = String.valueOf(element.getLineNumber());
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
        }

        // Capture MDC context for async execution
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        virtualThreadExecutor.submit(() -> {
            MDC.setContextMap(mdcContext);
            try {
                Logger structuredLogger = LoggerFactory.getLogger(loggerName);

                // Add structured fields to MDC
                MDC.put("service", service);
                MDC.put("eventType", eventType);
                MDC.put("lineNumber", lineNumber);
                MDC.put("version", "1.0.0");
                if (details != null) {
                    details.forEach((key, value) -> MDC.put(key, String.valueOf(value)));
                }

                if (throwable != null) {
                    MDC.put("error", throwable.getMessage());
                    MDC.put("exceptionClass", throwable.getClass().getName());
                    MDC.put("stackTrace", getStackTraceAsString(throwable));
                    structuredLogger.error(message, throwable);
                } else {
                    structuredLogger.info(message);
                }
            } finally {
                // Clear added MDC fields
                MDC.remove("service");
                MDC.remove("eventType");
                MDC.remove("lineNumber");
                MDC.remove("version");
                if (details != null) {
                    details.keySet().forEach(MDC::remove);
                }
                MDC.clear(); // Clear all to be safe
            }
        });
    }
}