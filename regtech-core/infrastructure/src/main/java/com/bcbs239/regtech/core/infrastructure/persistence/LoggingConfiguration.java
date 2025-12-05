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
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced logging configuration for GCP deployment with structured JSON logging.
 * Provides correlation ID tracking, event processing logging, and GCP-compatible log formats.
 * Uses ScopedValue for thread-safe context propagation with virtual threads.
 */
@Configuration
public class LoggingConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(LoggingConfiguration.class);

    // Synchronous simplified logging configuration.
    // Keep a simple ObjectMapper for structured JSON logs when enabled.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new MaybeJacksonModule());

    /**
     * If false, structured (JSON) logging is disabled and a simple plain-text
     * logging fallback will be used. Configured at bean initialization from
     * the Spring Environment (dev profile disables structured logging).
     */
    // Default to false (human-friendly) unless explicitly enabled by property
    // or when running in an explicit production profile. This avoids noisy
    // JSON output during local development by default.
    public static volatile boolean STRUCTURED_LOGGING_ENABLED = false;

    @Bean
    public ObjectMapper loggingObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new MaybeJacksonModule());
        return mapper;
    }

    @Autowired
    public void configureStructuredLogging(Environment env) {
        try {
            // Prefer an explicit property when present so environments can opt in/out
            Boolean prop = env.getProperty("logging.structured.enabled", Boolean.class);
            if (prop != null) {
                STRUCTURED_LOGGING_ENABLED = prop;
            } else {
                // Fallback to profile-based behavior: only enable structured logging
                // for explicit production-like profiles. This keeps local/dev output
                // human readable by default.
                List<String> profiles = Arrays.asList(env.getActiveProfiles());
                boolean isProd = profiles.contains("prod") || profiles.contains("production") || profiles.contains("gcp") || profiles.contains("stage") || profiles.contains("staging");
                STRUCTURED_LOGGING_ENABLED = isProd;
            }
        } catch (Exception e) {
            // default to enabled if we cannot determine the environment
            STRUCTURED_LOGGING_ENABLED = true;
        }
        logger.info("Structured logging enabled: {}", STRUCTURED_LOGGING_ENABLED);
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
        // No async executor to shut down in simplified configuration.
        logger.debug("LoggingConfiguration.shutdown() called - no async executor to stop.");
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
            
            // Set MDC for the request scope
            MDC.put("correlationId", finalCorrelationId);
            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestUri", request.getRequestURI());
            MDC.put("userAgent", request.getHeader("User-Agent"));
            
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
        return MDC.get("correlationId");
    }

    /**
     * Create a structured log entry for GCP
     */
    public static Map<String, Object> createStructuredLog(String eventType, Map<String, Object> details) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", System.currentTimeMillis());
        logEntry.put("eventType", eventType);
        logEntry.put("correlationId", getCurrentCorrelationId());
    String svc = MDC.get("service");
    logEntry.put("service", svc != null ? svc : "regtech");
    String ver = MDC.get("version");
    logEntry.put("version", ver != null ? ver : "1.0.0");

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
        if (throwable == null) return "";
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Extract simple class name from fully qualified class name
     */
    private static String getSimpleClassName(String fullyQualifiedName) {
        if (fullyQualifiedName == null) return "unknown";
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
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
    // Capture calling class and event type before execution
    final String loggerName;
    final String eventType;
    final Map<String, Object> capturedDetails;
        
        {
            String tempLoggerName = "com.bcbs239.regtech.structured";
            String tempEventType = details != null && details.containsKey("eventType") 
                ? String.valueOf(details.get("eventType")) : message;
            Map<String, Object> tempDetails = new HashMap<>();
            if (details != null) {
                tempDetails.putAll(details);
            }

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                // Skip LoggingConfiguration, LoggingService, and JDK classes
                if (!className.startsWith("com.bcbs239.regtech.core.infrastructure.persistence.LoggingConfiguration") &&
                    !className.startsWith("com.bcbs239.regtech.core.infrastructure.logging.LoggingService") &&
                    !className.startsWith("java.") &&
                    !className.startsWith("jdk.") &&
                    !className.startsWith("sun.")) {
                    tempLoggerName = className;
                    // Also capture the method name and line number for better traceability
                    tempDetails.put("sourceClass", className);
                    tempDetails.put("sourceMethod", element.getMethodName());
                    tempDetails.put("sourceLineNumber", element.getLineNumber());
                    break;
                }
            }
            loggerName = tempLoggerName;
            eventType = tempEventType;
            capturedDetails = tempDetails;
        }

        // If structured logging is disabled, emit a plain log message with details
        Logger targetLogger = LoggerFactory.getLogger(loggerName);
        if (!STRUCTURED_LOGGING_ENABLED) {
            // Format: [EventType] message - source=ClassName.methodName:line - details
            String sourceInfo = capturedDetails.containsKey("sourceClass") 
                ? String.format("%s.%s:%s", 
                    getSimpleClassName((String) capturedDetails.get("sourceClass")),
                    capturedDetails.get("sourceMethod"),
                    capturedDetails.get("sourceLineNumber"))
                : "unknown";
            
            String logMessage = String.format("[%s] %s - source=%s", eventType, message, sourceInfo);
            
            if (!capturedDetails.isEmpty()) {
                // Remove source info from details map for cleaner output
                Map<String, Object> displayDetails = new HashMap<>(capturedDetails);
                displayDetails.remove("sourceClass");
                displayDetails.remove("sourceMethod");
                displayDetails.remove("sourceLineNumber");
                
                if (!displayDetails.isEmpty()) {
                    logMessage += " - details=" + displayDetails.toString();
                }
            }
            
            if (throwable != null) targetLogger.error(logMessage, throwable);
            else targetLogger.info(logMessage);
            return;
        }

        // Structured logging: build a JSON object and log it synchronously
        try {
            Map<String, Object> entry = createStructuredLog(eventType, capturedDetails);
            entry.put("message", message);
            if (throwable != null) {
                entry.put("exception", getStackTraceAsString(throwable));
            }
            String json = OBJECT_MAPPER.writeValueAsString(entry);
            targetLogger.info(json);
        } catch (Exception e) {
            // Fallback to simple logging if JSON serialization fails
            targetLogger.warn("Failed to write structured log as JSON, falling back. Error: {}", e.getMessage());
            if (throwable != null) targetLogger.error(message, throwable);
            else targetLogger.info(message + " - details=" + capturedDetails.toString());
        }
    }

    /**
     * Execute code with a specific correlation ID in scope
     * Useful for background tasks or async operations
     */
    public static void withCorrelationId(String correlationId, Runnable task) {
        MDC.put("correlationId", correlationId);
        try {
            task.run();
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Execute code with full logging context
     */
    public static <T> T withLoggingContext(String correlationId, String service, 
                                          Map<String, Object> details, 
                                          java.util.function.Supplier<T> task) {
        MDC.put("correlationId", correlationId);
        MDC.put("service", service);
        try {
            return task.get();
        } finally {
            MDC.clear();
        }
    }
}

