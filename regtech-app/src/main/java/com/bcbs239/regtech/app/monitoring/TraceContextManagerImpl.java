package com.bcbs239.regtech.app.monitoring;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of TraceContextManager using Spring Boot 4's Micrometer Tracing.
 * 
 * This implementation leverages Spring Boot 4's auto-configured tracing infrastructure
 * to provide business logic access to trace context without manual span management.
 * 
 * Requirements: 1.1, 1.2 - Business context in traces
 */
@Component
public class TraceContextManagerImpl implements TraceContextManager {

    private static final Logger logger = LoggerFactory.getLogger(TraceContextManagerImpl.class);
    
    private final Tracer tracer;

    public TraceContextManagerImpl(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public String getCurrentTraceId() {
        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                return currentSpan.context().traceId();
            }
        } catch (Exception e) {
            logger.debug("Error getting current trace ID: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public String getCurrentSpanId() {
        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null && currentSpan.context() != null) {
                return currentSpan.context().spanId();
            }
        } catch (Exception e) {
            logger.debug("Error getting current span ID: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean hasActiveTrace() {
        try {
            Span currentSpan = tracer.currentSpan();
            return currentSpan != null && currentSpan.context() != null;
        } catch (Exception e) {
            logger.debug("Error checking active trace: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getFormattedTraceContext() {
        try {
            String traceId = getCurrentTraceId();
            String spanId = getCurrentSpanId();
            
            if (traceId != null && spanId != null) {
                return String.format("traceId=%s,spanId=%s", traceId, spanId);
            } else if (traceId != null) {
                return String.format("traceId=%s", traceId);
            }
        } catch (Exception e) {
            logger.debug("Error formatting trace context: {}", e.getMessage());
        }
        return "";
    }

    @Override
    public void addBusinessContext(String key, String value) {
        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                // Add as a tag to the current span
                currentSpan.tag(key, value);
                logger.debug("Added business context to span: {}={}", key, value);
            } else {
                logger.debug("No active span to add business context: {}={}", key, value);
            }
        } catch (Exception e) {
            logger.debug("Error adding business context: {}", e.getMessage());
        }
    }

    @Override
    public String getBusinessContext(String key) {
        try {
            Span currentSpan = tracer.currentSpan();
            if (currentSpan != null) {
                // Note: Micrometer Tracing doesn't provide direct access to tags
                // This would need to be implemented using a custom span processor
                // or by maintaining a separate context map
                logger.debug("Getting business context not directly supported by Micrometer Tracing: {}", key);
                return null;
            }
        } catch (Exception e) {
            logger.debug("Error getting business context: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Convenience method to add multiple business context entries.
     * 
     * @param contextMap Map of context key-value pairs
     */
    public void addBusinessContextMap(java.util.Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : contextMap.entrySet()) {
                addBusinessContext(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Adds business context with a namespace prefix.
     * 
     * @param namespace The namespace prefix (e.g., "business", "user", "batch")
     * @param key The context key
     * @param value The context value
     */
    public void addNamespacedBusinessContext(String namespace, String key, String value) {
        String namespacedKey = namespace + "." + key;
        addBusinessContext(namespacedKey, value);
    }

    /**
     * Adds user context to the current span.
     * 
     * @param userId The user ID
     * @param userRole The user role
     */
    public void addUserContext(String userId, String userRole) {
        addBusinessContext("business.user.id", userId);
        addBusinessContext("business.user.role", userRole);
    }

    /**
     * Adds batch context to the current span.
     * 
     * @param batchId The batch ID
     * @param batchType The batch type
     */
    public void addBatchContext(String batchId, String batchType) {
        addBusinessContext("business.batch.id", batchId);
        addBusinessContext("business.batch.type", batchType);
    }

    /**
     * Adds portfolio context to the current span.
     * 
     * @param portfolioId The portfolio ID
     * @param portfolioType The portfolio type
     */
    public void addPortfolioContext(String portfolioId, String portfolioType) {
        addBusinessContext("business.portfolio.id", portfolioId);
        addBusinessContext("business.portfolio.type", portfolioType);
    }

    /**
     * Adds tenant context to the current span.
     * 
     * @param tenantId The tenant ID
     * @param organizationId The organization ID
     */
    public void addTenantContext(String tenantId, String organizationId) {
        addBusinessContext("business.tenant.id", tenantId);
        addBusinessContext("business.organization.id", organizationId);
    }

    /**
     * Adds operation context to the current span.
     * 
     * @param operationType The operation type (e.g., "create", "update", "process")
     * @param operationName The operation name
     */
    public void addOperationContext(String operationType, String operationName) {
        addBusinessContext("business.operation.type", operationType);
        addBusinessContext("business.operation.name", operationName);
    }

    /**
     * Adds error context to the current span.
     * 
     * @param errorCode The business error code
     * @param errorCategory The error category
     */
    public void addErrorContext(String errorCode, String errorCategory) {
        addBusinessContext("business.error.code", errorCode);
        addBusinessContext("business.error.category", errorCategory);
    }

    /**
     * Adds performance context to the current span.
     * 
     * @param performanceCategory The performance category (e.g., "fast", "slow", "critical")
     * @param recordCount The number of records processed
     */
    public void addPerformanceContext(String performanceCategory, Long recordCount) {
        addBusinessContext("business.performance.category", performanceCategory);
        if (recordCount != null) {
            addBusinessContext("business.performance.record_count", recordCount.toString());
        }
    }
}