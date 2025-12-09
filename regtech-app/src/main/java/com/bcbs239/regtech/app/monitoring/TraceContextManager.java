package com.bcbs239.regtech.app.monitoring;

/**
 * Simple interface for querying current trace context for business logic.
 * 
 * This interface provides access to trace information without requiring
 * manual span creation, as Spring Boot 4 handles trace management automatically.
 * 
 * Requirements: 1.1, 1.2 - Business context in traces
 */
public interface TraceContextManager {
    
    /**
     * Gets the current trace ID if available.
     * 
     * @return The current trace ID, or null if no active trace
     */
    String getCurrentTraceId();
    
    /**
     * Gets the current span ID if available.
     * 
     * @return The current span ID, or null if no active span
     */
    String getCurrentSpanId();
    
    /**
     * Checks if there is an active trace context.
     * 
     * @return true if there is an active trace, false otherwise
     */
    boolean hasActiveTrace();
    
    /**
     * Gets the current trace context as a formatted string for logging.
     * Format: "traceId=<traceId>,spanId=<spanId>"
     * 
     * @return Formatted trace context string, or empty string if no active trace
     */
    String getFormattedTraceContext();
    
    /**
     * Adds business context to the current span if available.
     * This is a convenience method for adding business-specific tags.
     * 
     * @param key The context key
     * @param value The context value
     */
    void addBusinessContext(String key, String value);
    
    /**
     * Gets business context from the current span if available.
     * 
     * @param key The context key
     * @return The context value, or null if not found or no active span
     */
    String getBusinessContext(String key);
}