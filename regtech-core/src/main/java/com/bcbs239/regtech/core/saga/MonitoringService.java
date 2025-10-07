package com.bcbs239.regtech.core.saga;

/**
 * Monitoring service interface for saga observability.
 * Provides logging, metrics, and alerting capabilities.
 */
public interface MonitoringService {

    /**
     * Records when a saga starts
     */
    void recordSagaStarted(String sagaId, String sagaType);

    /**
     * Records when a saga completes successfully
     */
    void recordSagaCompleted(String sagaId);

    /**
     * Records when a saga starts compensation
     */
    void recordSagaCompensating(String sagaId, String reason);

    /**
     * Records when a saga compensation completes
     */
    void recordSagaCompensated(String sagaId);

    /**
     * Records when a saga fails completely
     */
    void recordSagaFailed(String sagaId, String errorMessage);

    /**
     * Records a saga step execution
     */
    void recordSagaStep(String sagaId, String stepName, boolean success, long durationMs);

    /**
     * Records a message sent between bounded contexts
     */
    void recordMessageSent(String sagaId, String messageType, String target);

    /**
     * Records a message received from another bounded context
     */
    void recordMessageReceived(String sagaId, String messageType, String source);

    /**
     * Records a business timeout event
     */
    void recordTimeout(String sagaId, String timeoutType);

    /**
     * Increments a custom metric
     */
    void incrementMetric(String metricName, String... tags);

    /**
     * Records a gauge value
     */
    void recordGauge(String metricName, double value, String... tags);
}