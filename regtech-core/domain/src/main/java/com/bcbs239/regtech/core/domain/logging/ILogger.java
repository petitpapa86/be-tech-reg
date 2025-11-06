package com.bcbs239.regtech.core.domain.logging;

import java.util.Map;

/**
 * Domain interface for logging operations.
 * Provides abstraction over logging infrastructure for clean architecture compliance.
 */
public interface ILogger {

    /**
     * Log a structured message asynchronously.
     * @param message the log message
     * @param details additional details to include in the log entry
     */
    void asyncStructuredLog(String message, Map<String, Object> details);

    /**
     * Log a structured error message asynchronously.
     * @param message the log message
     * @param throwable the exception that occurred
     * @param details additional details to include in the log entry
     */
    void asyncStructuredErrorLog(String message, Throwable throwable, Map<String, Object> details);
}

