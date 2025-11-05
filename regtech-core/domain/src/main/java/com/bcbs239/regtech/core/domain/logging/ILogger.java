package com.bcbs239.regtech.core.domain.logging;

import java.util.Map;

/**
 * Domain interface for logging operations.
 * Provides abstraction over logging infrastructure for clean architecture compliance.
 */
public interface ILogger {

    /**
     * Create a structured log entry with the given event type and details.
     * @param eventType the type of event being logged
     * @param details additional details to include in the log entry
     * @return a map representing the structured log entry
     */
    Map<String, Object> createStructuredLog(String eventType, Map<String, Object> details);
}