package com.bcbs239.regtech.core.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * GCP-based monitoring service implementation.
 * Provides logging and could be extended to use Cloud Logging, Cloud Monitoring, etc.
 */
@Service
public class GcpMonitoringService implements MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(GcpMonitoringService.class);

    @Override
    public void recordSagaStarted(String sagaId, String sagaType) {
        logger.info("Saga started - ID: {}, Type: {}", sagaId, sagaType);
        // TODO: Send to Cloud Logging/Monitoring
    }

    @Override
    public void recordSagaCompleted(String sagaId) {
        logger.info("Saga completed successfully - ID: {}", sagaId);
        // TODO: Send to Cloud Monitoring
    }

    @Override
    public void recordSagaCompensating(String sagaId, String reason) {
        logger.warn("Saga compensation started - ID: {}, Reason: {}", sagaId, reason);
        // TODO: Send to Cloud Logging with WARNING level
    }

    @Override
    public void recordSagaCompensated(String sagaId) {
        logger.info("Saga compensation completed - ID: {}", sagaId);
        // TODO: Send to Cloud Monitoring
    }

    @Override
    public void recordSagaFailed(String sagaId, String errorMessage) {
        logger.error("Saga failed - ID: {}, Error: {}", sagaId, errorMessage);
        // TODO: Send to Cloud Logging with ERROR level and create alert
    }

    @Override
    public void recordSagaStep(String sagaId, String stepName, boolean success, long durationMs) {
        if (success) {
            logger.debug("Saga step completed - ID: {}, Step: {}, Duration: {}ms", sagaId, stepName, durationMs);
        } else {
            logger.warn("Saga step failed - ID: {}, Step: {}, Duration: {}ms", sagaId, stepName, durationMs);
        }
        // TODO: Send metrics to Cloud Monitoring
    }

    @Override
    public void recordMessageSent(String sagaId, String messageType, String target) {
        logger.debug("Message sent - Saga: {}, Type: {}, Target: {}", sagaId, messageType, target);
        // TODO: Send to Cloud Logging
    }

    @Override
    public void recordMessageReceived(String sagaId, String messageType, String source) {
        logger.debug("Message received - Saga: {}, Type: {}, Source: {}", sagaId, messageType, source);
        // TODO: Send to Cloud Logging
    }

    @Override
    public void recordTimeout(String sagaId, String timeoutType) {
        logger.warn("Saga timeout - ID: {}, Type: {}", sagaId, timeoutType);
        // TODO: Send to Cloud Monitoring and create alert
    }

    @Override
    public void incrementMetric(String metricName, String... tags) {
        logger.debug("Metric incremented - Name: {}, Tags: {}", metricName, tags);
        // TODO: Send to Cloud Monitoring
    }

    @Override
    public void recordGauge(String metricName, double value, String... tags) {
        logger.debug("Gauge recorded - Name: {}, Value: {}, Tags: {}", metricName, value, tags);
        // TODO: Send to Cloud Monitoring
    }
}