package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Throughput and Capacity Monitor for tracking transaction volumes and identifying bottlenecks.
 * 
 * This component:
 * - Tracks transaction volume against capacity targets
 * - Identifies performance bottlenecks
 * - Provides performance optimization recommendations
 * - Records SLA breaches with impact assessment and root cause analysis
 * 
 * Requirements: 8.4, 8.5 - Throughput capacity tracking and SLA breach recording
 */
@Component
public class ThroughputCapacityMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ThroughputCapacityMonitor.class);

    private final MeterRegistry meterRegistry;
    private final Map<String, ComponentThroughputTracker> componentTrackers = new ConcurrentHashMap<>();
    private final List<SLABreachRecord> breachHistory = new ArrayList<>();
    
    // Capacity targets (configurable via properties in production)
    private static final Map<String, Long> CAPACITY_TARGETS = Map.of(
        "api", 10000L,              // 10k requests per hour
        "batch", 100L,              // 100 batches per hour
        "risk-calculation", 5000L,  // 5k calculations per hour
        "data-quality", 5000L,      // 5k validations per hour
        "report-generation", 500L   // 500 reports per hour
    );
    
    // Bottleneck detection thresholds
    private static final double HIGH_UTILIZATION_THRESHOLD = 0.80; // 80%
    private static final double CRITICAL_UTILIZATION_THRESHOLD = 0.95; // 95%
    private static final int BOTTLENECK_DETECTION_WINDOW_MINUTES = 15;
    
    private final Counter slaBreachCounter;

    public ThroughputCapacityMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.slaBreachCounter = Counter.builder("throughput.sla.breach")
            .description("Number of throughput SLA breaches")
            .register(meterRegistry);
    }

    /**
     * Records transaction volume for a component.
     * 
     * @param component The component name
     * @param transactionCount Number of transactions in the current period
     * @param periodDuration Duration of the measurement period
     */
    public void recordThroughput(String component, long transactionCount, Duration periodDuration) {
        ComponentThroughputTracker tracker = componentTrackers.computeIfAbsent(component, key -> {
            ComponentThroughputTracker newTracker = new ComponentThroughputTracker(
                component, 
                CAPACITY_TARGETS.getOrDefault(component, 1000L)
            );
            registerThroughputMetrics(newTracker);
            return newTracker;
        });
        
        tracker.recordThroughput(transactionCount, periodDuration);
        
        // Check for capacity issues
        checkCapacityUtilization(tracker);
    }

    /**
     * Records a single transaction for a component.
     * 
     * @param component The component name
     */
    public void recordTransaction(String component) {
        ComponentThroughputTracker tracker = componentTrackers.computeIfAbsent(component, key -> {
            ComponentThroughputTracker newTracker = new ComponentThroughputTracker(
                component,
                CAPACITY_TARGETS.getOrDefault(component, 1000L)
            );
            registerThroughputMetrics(newTracker);
            return newTracker;
        });
        
        tracker.incrementTransactionCount();
    }

    /**
     * Identifies performance bottlenecks across all components.
     * 
     * @return List of identified bottlenecks
     */
    public List<PerformanceBottleneck> identifyBottlenecks() {
        List<PerformanceBottleneck> bottlenecks = new ArrayList<>();
        
        for (ComponentThroughputTracker tracker : componentTrackers.values()) {
            double utilization = tracker.getCapacityUtilization();
            
            if (utilization >= CRITICAL_UTILIZATION_THRESHOLD) {
                bottlenecks.add(new PerformanceBottleneck(
                    tracker.component,
                    BottleneckSeverity.CRITICAL,
                    utilization,
                    tracker.getCurrentThroughput(),
                    tracker.capacityTarget,
                    "Critical capacity utilization - immediate action required",
                    generateOptimizationRecommendations(tracker, BottleneckSeverity.CRITICAL)
                ));
            } else if (utilization >= HIGH_UTILIZATION_THRESHOLD) {
                bottlenecks.add(new PerformanceBottleneck(
                    tracker.component,
                    BottleneckSeverity.HIGH,
                    utilization,
                    tracker.getCurrentThroughput(),
                    tracker.capacityTarget,
                    "High capacity utilization - scaling recommended",
                    generateOptimizationRecommendations(tracker, BottleneckSeverity.HIGH)
                ));
            }
        }
        
        return bottlenecks;
    }

    /**
     * Records an SLA breach with detailed impact assessment and root cause analysis.
     * 
     * @param component The component that breached SLA
     * @param breachType Type of breach (throughput, latency, etc.)
     * @param actualValue Actual measured value
     * @param thresholdValue SLA threshold value
     * @param impactAssessment Assessment of business impact
     * @param rootCause Identified root cause
     */
    public void recordSLABreach(String component, String breachType, long actualValue, 
                               long thresholdValue, String impactAssessment, String rootCause) {
        SLABreachRecord breach = new SLABreachRecord(
            Instant.now(),
            component,
            breachType,
            actualValue,
            thresholdValue,
            impactAssessment,
            rootCause,
            calculateBreachSeverity(actualValue, thresholdValue),
            determineBreachSeverity(calculateBreachSeverity(actualValue, thresholdValue))
        );
        
        breachHistory.add(breach);
        slaBreachCounter.increment();
        
        logger.error("SLA breach recorded: component={}, type={}, actual={}, threshold={}, impact={}, rootCause={}",
            component, breachType, actualValue, thresholdValue, impactAssessment, rootCause);
        
        // Record breach metrics
        Counter.builder("sla.breach.detailed")
            .description("Detailed SLA breach tracking")
            .tag("component", component)
            .tag("breach.type", breachType)
            .tag("severity", breach.severity.name())
            .register(meterRegistry)
            .increment();
            
        Gauge.builder("sla.breach.severity.score", breach.severityScore, Number::doubleValue)
            .description("SLA breach severity score")
            .tags("component", component, "breach.type", breachType)
            .register(meterRegistry);
    }

    /**
     * Gets throughput metrics for a component.
     * 
     * @param component The component name
     * @return Throughput metrics or null if not tracked
     */
    public ThroughputMetrics getThroughputMetrics(String component) {
        ComponentThroughputTracker tracker = componentTrackers.get(component);
        if (tracker == null) {
            return null;
        }
        
        return new ThroughputMetrics(
            component,
            tracker.getCurrentThroughput(),
            tracker.getPeakThroughput(),
            tracker.getAverageThroughput(),
            tracker.getCapacityUtilization(),
            tracker.capacityTarget,
            tracker.getTotalTransactions()
        );
    }

    /**
     * Gets capacity utilization report for all components.
     * 
     * @return Capacity utilization report
     */
    public CapacityUtilizationReport getCapacityUtilizationReport() {
        Map<String, Double> utilizationByComponent = new ConcurrentHashMap<>();
        double totalUtilization = 0.0;
        int componentCount = 0;
        
        for (ComponentThroughputTracker tracker : componentTrackers.values()) {
            double utilization = tracker.getCapacityUtilization();
            utilizationByComponent.put(tracker.component, utilization);
            totalUtilization += utilization;
            componentCount++;
        }
        
        double averageUtilization = componentCount > 0 ? totalUtilization / componentCount : 0.0;
        
        return new CapacityUtilizationReport(
            utilizationByComponent,
            averageUtilization,
            identifyBottlenecks(),
            generateSystemWideRecommendations()
        );
    }

    /**
     * Gets SLA breach history.
     * 
     * @param component Optional component filter (null for all)
     * @param hours Number of hours of history to retrieve
     * @return List of SLA breaches
     */
    public List<SLABreachRecord> getSLABreachHistory(String component, int hours) {
        Instant cutoff = Instant.now().minus(Duration.ofHours(hours));
        
        return breachHistory.stream()
            .filter(breach -> breach.timestamp.isAfter(cutoff))
            .filter(breach -> component == null || breach.component.equals(component))
            .toList();
    }

    /**
     * Scheduled task to detect and log bottlenecks.
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void detectAndLogBottlenecks() {
        List<PerformanceBottleneck> bottlenecks = identifyBottlenecks();
        
        if (!bottlenecks.isEmpty()) {
            logger.warn("Performance bottlenecks detected: count={}", bottlenecks.size());
            for (PerformanceBottleneck bottleneck : bottlenecks) {
                logger.warn("Bottleneck: component={}, severity={}, utilization={}%, recommendations={}",
                    bottleneck.component, bottleneck.severity, 
                    bottleneck.utilizationPercentage * 100, bottleneck.recommendations);
            }
        }
    }

    /**
     * Scheduled task to clean up old breach history.
     */
    @Scheduled(cron = "0 0 3 * * *") // Run at 3 AM daily
    public void cleanupBreachHistory() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        breachHistory.removeIf(breach -> breach.timestamp.isBefore(cutoff));
        logger.info("Cleaned up SLA breach history older than 90 days");
    }

    /**
     * Checks capacity utilization and records breaches if needed.
     */
    private void checkCapacityUtilization(ComponentThroughputTracker tracker) {
        double utilization = tracker.getCapacityUtilization();
        
        if (utilization > 1.0) {
            // Capacity exceeded
            String impactAssessment = String.format(
                "Component %s exceeded capacity by %.1f%%. Potential service degradation.",
                tracker.component, (utilization - 1.0) * 100
            );
            
            String rootCause = determineRootCause(tracker);
            
            recordSLABreach(
                tracker.component,
                "capacity_exceeded",
                tracker.getCurrentThroughput(),
                tracker.capacityTarget,
                impactAssessment,
                rootCause
            );
        }
    }

    /**
     * Determines root cause of capacity issues.
     */
    private String determineRootCause(ComponentThroughputTracker tracker) {
        double utilizationTrend = tracker.getUtilizationTrend();
        
        if (utilizationTrend > 0.1) {
            return "Rapid increase in transaction volume - possible traffic spike";
        } else if (utilizationTrend > 0.05) {
            return "Gradual increase in transaction volume - organic growth";
        } else {
            return "Sustained high transaction volume - capacity planning needed";
        }
    }

    /**
     * Calculates breach severity score.
     */
    private double calculateBreachSeverity(long actualValue, long thresholdValue) {
        return (double) actualValue / thresholdValue;
    }

    /**
     * Determines breach severity level based on severity score.
     */
    private BottleneckSeverity determineBreachSeverity(double severityScore) {
        return severityScore > 1.5 ? BottleneckSeverity.CRITICAL : BottleneckSeverity.HIGH;
    }

    /**
     * Generates optimization recommendations for a component.
     */
    private List<String> generateOptimizationRecommendations(ComponentThroughputTracker tracker, 
                                                             BottleneckSeverity severity) {
        List<String> recommendations = new ArrayList<>();
        
        if (severity == BottleneckSeverity.CRITICAL) {
            recommendations.add("IMMEDIATE: Scale up component capacity");
            recommendations.add("IMMEDIATE: Enable request throttling to prevent overload");
            recommendations.add("IMMEDIATE: Review and optimize critical code paths");
        } else {
            recommendations.add("Plan capacity increase for " + tracker.component);
            recommendations.add("Review performance metrics and identify optimization opportunities");
        }
        
        // Component-specific recommendations
        switch (tracker.component) {
            case "api":
                recommendations.add("Consider implementing caching for frequently accessed data");
                recommendations.add("Review database query performance");
                break;
            case "batch":
                recommendations.add("Optimize batch processing parallelization");
                recommendations.add("Review batch size configuration");
                break;
            case "risk-calculation":
                recommendations.add("Review calculation algorithm efficiency");
                recommendations.add("Consider implementing result caching");
                break;
            case "data-quality":
                recommendations.add("Optimize rule execution order");
                recommendations.add("Consider parallel rule evaluation");
                break;
            case "report-generation":
                recommendations.add("Implement report template caching");
                recommendations.add("Optimize data aggregation queries");
                break;
        }
        
        return recommendations;
    }

    /**
     * Generates system-wide optimization recommendations.
     */
    private List<String> generateSystemWideRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        double avgUtilization = componentTrackers.values().stream()
            .mapToDouble(ComponentThroughputTracker::getCapacityUtilization)
            .average()
            .orElse(0.0);
        
        if (avgUtilization > 0.8) {
            recommendations.add("System-wide capacity planning recommended");
            recommendations.add("Consider horizontal scaling across all components");
        } else if (avgUtilization > 0.6) {
            recommendations.add("Monitor capacity trends closely");
            recommendations.add("Prepare scaling plan for anticipated growth");
        } else {
            recommendations.add("Current capacity is adequate");
            recommendations.add("Continue monitoring for changes in usage patterns");
        }
        
        return recommendations;
    }

    /**
     * Registers throughput metrics with the meter registry.
     */
    private void registerThroughputMetrics(ComponentThroughputTracker tracker) {
        Gauge.builder("throughput.current", tracker, ComponentThroughputTracker::getCurrentThroughput)
            .description("Current throughput")
            .tags("component", tracker.component)
            .register(meterRegistry);
            
        Gauge.builder("throughput.capacity.utilization", tracker, t -> t.getCapacityUtilization() * 100)
            .description("Capacity utilization percentage")
            .tags("component", tracker.component)
            .register(meterRegistry);
            
        Gauge.builder("throughput.capacity.target", tracker, t -> t.capacityTarget)
            .description("Capacity target")
            .tags("component", tracker.component)
            .register(meterRegistry);
    }

    // Inner classes
    
    private static class ComponentThroughputTracker {
        private final String component;
        private final long capacityTarget;
        private final AtomicLong currentThroughput = new AtomicLong(0);
        private final AtomicLong peakThroughput = new AtomicLong(0);
        private final AtomicLong totalTransactions = new AtomicLong(0);
        private final List<ThroughputMeasurement> measurements = new ArrayList<>();
        private Instant lastMeasurementTime = Instant.now();
        
        public ComponentThroughputTracker(String component, long capacityTarget) {
            this.component = component;
            this.capacityTarget = capacityTarget;
        }
        
        public void recordThroughput(long transactionCount, Duration periodDuration) {
            // Normalize to transactions per hour
            long throughputPerHour = (long) (transactionCount * 3600.0 / periodDuration.getSeconds());
            
            currentThroughput.set(throughputPerHour);
            totalTransactions.addAndGet(transactionCount);
            
            // Update peak
            long current = currentThroughput.get();
            long peak = peakThroughput.get();
            if (current > peak) {
                peakThroughput.compareAndSet(peak, current);
            }
            
            // Record measurement
            measurements.add(new ThroughputMeasurement(Instant.now(), throughputPerHour));
            lastMeasurementTime = Instant.now();
            
            // Keep only recent measurements (last 24 hours)
            Instant cutoff = Instant.now().minus(Duration.ofHours(24));
            measurements.removeIf(m -> m.timestamp.isBefore(cutoff));
        }
        
        public void incrementTransactionCount() {
            totalTransactions.incrementAndGet();
            
            // Update current throughput based on time window
            Instant now = Instant.now();
            long secondsSinceLastMeasurement = Duration.between(lastMeasurementTime, now).getSeconds();
            
            if (secondsSinceLastMeasurement > 0) {
                long throughputPerHour = (long) (totalTransactions.get() * 3600.0 / secondsSinceLastMeasurement);
                currentThroughput.set(throughputPerHour);
            }
        }
        
        public long getCurrentThroughput() {
            return currentThroughput.get();
        }
        
        public long getPeakThroughput() {
            return peakThroughput.get();
        }
        
        public double getAverageThroughput() {
            if (measurements.isEmpty()) return 0.0;
            return measurements.stream()
                .mapToLong(ThroughputMeasurement::throughput)
                .average()
                .orElse(0.0);
        }
        
        public double getCapacityUtilization() {
            return (double) currentThroughput.get() / capacityTarget;
        }
        
        public long getTotalTransactions() {
            return totalTransactions.get();
        }
        
        public double getUtilizationTrend() {
            if (measurements.size() < 2) return 0.0;
            
            // Compare first half vs second half of measurements
            int midpoint = measurements.size() / 2;
            double firstHalfAvg = measurements.subList(0, midpoint).stream()
                .mapToDouble(m -> (double) m.throughput / capacityTarget)
                .average()
                .orElse(0.0);
            double secondHalfAvg = measurements.subList(midpoint, measurements.size()).stream()
                .mapToDouble(m -> (double) m.throughput / capacityTarget)
                .average()
                .orElse(0.0);
                
            return secondHalfAvg - firstHalfAvg;
        }
    }
    
    private record ThroughputMeasurement(
        Instant timestamp,
        long throughput
    ) {}
    
    public enum BottleneckSeverity {
        HIGH,
        CRITICAL
    }
    
    public record PerformanceBottleneck(
        String component,
        BottleneckSeverity severity,
        double utilizationPercentage,
        long currentThroughput,
        long capacityTarget,
        String description,
        List<String> recommendations
    ) {}
    
    public record ThroughputMetrics(
        String component,
        long currentThroughput,
        long peakThroughput,
        double averageThroughput,
        double capacityUtilization,
        long capacityTarget,
        long totalTransactions
    ) {}
    
    public record CapacityUtilizationReport(
        Map<String, Double> utilizationByComponent,
        double averageUtilization,
        List<PerformanceBottleneck> bottlenecks,
        List<String> systemWideRecommendations
    ) {}
    
    public record SLABreachRecord(
        Instant timestamp,
        String component,
        String breachType,
        long actualValue,
        long thresholdValue,
        String impactAssessment,
        String rootCause,
        double severityScore,
        BottleneckSeverity severity
    ) {}
}
