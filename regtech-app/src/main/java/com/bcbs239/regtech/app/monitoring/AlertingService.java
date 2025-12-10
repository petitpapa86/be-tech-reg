package com.bcbs239.regtech.app.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Core alerting service for monitoring and notification.
 * Manages alert rules, evaluates conditions, and triggers notifications.
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Service
public class AlertingService {

    private static final Logger logger = LoggerFactory.getLogger(AlertingService.class);

    private final NotificationService notificationService;
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();
    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();

    public AlertingService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Adds an alert rule to the service.
     */
    public void addAlertRule(AlertRule rule) {
        alertRules.put(rule.getId(), rule);
        logger.info("Added alert rule: {}", rule.getName());
    }

    /**
     * Evaluates all alert rules and triggers notifications for active alerts.
     */
    public void evaluateAlerts() {
        for (AlertRule rule : alertRules.values()) {
            try {
                if (rule.evaluate()) {
                    AlertState state = alertStates.computeIfAbsent(rule.getId(),
                        id -> new AlertState());

                    if (!state.isActive()) {
                        // New alert
                        Alert alert = new Alert(
                            rule.getId(),
                            rule.getName(),
                            rule.getSeverity(),
                            rule.getDescription(),
                            Instant.now(),
                            rule.getMetrics()
                        );

                        notificationService.sendAlert(alert);
                        state.setActive(true);
                        state.setLastTriggered(Instant.now());
                        logger.warn("Alert triggered: {}", rule.getName());
                    }
                } else {
                    // Clear alert if it was active
                    AlertState state = alertStates.get(rule.getId());
                    if (state != null && state.isActive()) {
                        state.setActive(false);
                        logger.info("Alert cleared: {}", rule.getName());
                    }
                }
            } catch (Exception e) {
                logger.error("Error evaluating alert rule: {}", rule.getName(), e);
            }
        }
    }

    /**
     * Alert severity levels.
     */
    public enum AlertSeverity {
        INFO, WARNING, CRITICAL
    }

    /**
     * Alert rule definition.
     */
    public static class AlertRule {
        private final String id;
        private final String name;
        private final AlertSeverity severity;
        private final String description;
        private final Predicate<Void> condition;
        private final Map<String, Object> metrics;

        public AlertRule(String id, String name, AlertSeverity severity,
                        String description, Predicate<Void> condition,
                        Map<String, Object> metrics) {
            this.id = id;
            this.name = name;
            this.severity = severity;
            this.description = description;
            this.condition = condition;
            this.metrics = metrics != null ? metrics : new HashMap<>();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public AlertSeverity getSeverity() { return severity; }
        public String getDescription() { return description; }
        public Map<String, Object> getMetrics() { return metrics; }

        public boolean evaluate() {
            return condition.test(null);
        }
    }

    /**
     * Alert instance.
     */
    public static class Alert {
        private final String ruleId;
        private final String ruleName;
        private final AlertSeverity severity;
        private final String description;
        private final Instant timestamp;
        private final Map<String, Object> metrics;

        public Alert(String ruleId, String ruleName, AlertSeverity severity,
                    String description, Instant timestamp, Map<String, Object> metrics) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.severity = severity;
            this.description = description;
            this.timestamp = timestamp;
            this.metrics = metrics != null ? new HashMap<>(metrics) : new HashMap<>();
        }

        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public AlertSeverity getSeverity() { return severity; }
        public String getDescription() { return description; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, Object> getMetrics() { return metrics; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("ruleId", ruleId);
            map.put("ruleName", ruleName);
            map.put("severity", severity.toString());
            map.put("description", description);
            map.put("timestamp", timestamp.toString());
            map.put("metrics", metrics);
            return map;
        }
    }

    /**
     * Alert state tracking.
     */
    private static class AlertState {
        private boolean active;
        private Instant lastTriggered;

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public Instant getLastTriggered() { return lastTriggered; }
        public void setLastTriggered(Instant lastTriggered) { this.lastTriggered = lastTriggered; }
    }
}
