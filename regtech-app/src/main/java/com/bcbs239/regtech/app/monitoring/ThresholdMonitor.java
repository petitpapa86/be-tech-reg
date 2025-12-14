package com.bcbs239.regtech.app.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Threshold Monitor for tracking and evaluating various system thresholds.
 *
 * This component monitors different types of thresholds including:
 * - Error rates
 * - Performance metrics
 * - SLA compliance
 * - Resource utilization
 *
 * Requirements: 5.1 - Error rate threshold alerting
 */
@Component
public class ThresholdMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ThresholdMonitor.class);

    private final Map<String, ThresholdDefinition> thresholds = new ConcurrentHashMap<>();

    public ThresholdMonitor() {
        // Initialize default thresholds
        initializeDefaultThresholds();
    }

    private void initializeDefaultThresholds() {
        // Error rate thresholds
        thresholds.put("error-rate-critical", new ThresholdDefinition(0.05, ThresholdType.ERROR_RATE)); // 5%
        thresholds.put("error-rate-warning", new ThresholdDefinition(0.02, ThresholdType.ERROR_RATE));   // 2%

        // Performance thresholds
        thresholds.put("response-time-critical", new ThresholdDefinition(5000L, ThresholdType.RESPONSE_TIME)); // 5 seconds
        thresholds.put("response-time-warning", new ThresholdDefinition(2000L, ThresholdType.RESPONSE_TIME));   // 2 seconds

        // SLA thresholds
        thresholds.put("sla-breach", new ThresholdDefinition(0.99, ThresholdType.SLA_COMPLIANCE)); // 99% uptime
    }

    /**
     * Evaluate if a value exceeds a threshold
     */
    public ThresholdResult evaluate(String thresholdName, double value) {
        ThresholdDefinition threshold = thresholds.get(thresholdName);
        if (threshold == null) {
            logger.warn("Threshold '{}' not found", thresholdName);
            return ThresholdResult.UNKNOWN;
        }

        boolean exceeded = false;
        ThresholdSeverity severity = ThresholdSeverity.NORMAL;

        switch (threshold.getType()) {
            case ERROR_RATE:
            case SLA_COMPLIANCE:
                // For rates and percentages, higher values are worse
                exceeded = value > threshold.getValue();
                if (exceeded) {
                    severity = value > (threshold.getValue() * 2) ? ThresholdSeverity.CRITICAL : ThresholdSeverity.WARNING;
                }
                break;
            case RESPONSE_TIME:
                // For response times, higher values are worse
                exceeded = value > threshold.getValue();
                if (exceeded) {
                    severity = value > (threshold.getValue() * 2) ? ThresholdSeverity.CRITICAL : ThresholdSeverity.WARNING;
                }
                break;
        }

        return new ThresholdResult(exceeded, severity, threshold.getValue(), value);
    }

    /**
     * Check if a threshold is exceeded
     */
    public boolean isExceeded(String thresholdName, double value) {
        return evaluate(thresholdName, value).isExceeded();
    }

    /**
     * Add or update a threshold
     */
    public void setThreshold(String name, double value, ThresholdType type) {
        thresholds.put(name, new ThresholdDefinition(value, type));
        logger.info("Updated threshold '{}' to {} ({})", name, value, type);
    }

    /**
     * Get a threshold definition
     */
    public ThresholdDefinition getThreshold(String name) {
        return thresholds.get(name);
    }

    public enum ThresholdType {
        ERROR_RATE,
        RESPONSE_TIME,
        SLA_COMPLIANCE,
        RESOURCE_UTILIZATION
    }

    public enum ThresholdSeverity {
        NORMAL,
        WARNING,
        CRITICAL
    }

    public static class ThresholdDefinition {
        private final double value;
        private final ThresholdType type;

        public ThresholdDefinition(double value, ThresholdType type) {
            this.value = value;
            this.type = type;
        }

        public double getValue() {
            return value;
        }

        public ThresholdType getType() {
            return type;
        }
    }

    public static class ThresholdResult {
        public static final ThresholdResult UNKNOWN = new ThresholdResult(false, ThresholdSeverity.NORMAL, 0, 0);

        private final boolean exceeded;
        private final ThresholdSeverity severity;
        private final double thresholdValue;
        private final double actualValue;

        public ThresholdResult(boolean exceeded, ThresholdSeverity severity, double thresholdValue, double actualValue) {
            this.exceeded = exceeded;
            this.severity = severity;
            this.thresholdValue = thresholdValue;
            this.actualValue = actualValue;
        }

        public boolean isExceeded() {
            return exceeded;
        }

        public ThresholdSeverity getSeverity() {
            return severity;
        }

        public double getThresholdValue() {
            return thresholdValue;
        }

        public double getActualValue() {
            return actualValue;
        }
    }
}