package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service Availability Calculator for tracking uptime percentages and availability trends.
 * 
 * This component:
 * - Measures uptime percentage for each service component
 * - Maintains historical availability data with retention
 * - Detects availability trend degradation
 * - Provides availability reporting and analysis
 * 
 * Requirements: 8.3 - Service availability calculation
 */
@Component
public class ServiceAvailabilityCalculator {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAvailabilityCalculator.class);

    private final MeterRegistry meterRegistry;
    private final Map<String, ServiceAvailabilityData> availabilityData = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int HISTORICAL_DATA_RETENTION_DAYS = 90;
    private static final double DEGRADATION_THRESHOLD = 0.01; // 1% degradation
    private static final int TREND_ANALYSIS_WINDOW_HOURS = 24;

    public ServiceAvailabilityCalculator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Records a service availability check.
     * 
     * @param serviceName The service component name
     * @param isAvailable Whether the service is currently available
     * @param responseTimeMs Response time of the health check
     */
    public void recordAvailabilityCheck(String serviceName, boolean isAvailable, long responseTimeMs) {
        ServiceAvailabilityData data = availabilityData.computeIfAbsent(serviceName, key -> {
            ServiceAvailabilityData newData = new ServiceAvailabilityData(serviceName);
            registerAvailabilityMetrics(newData);
            return newData;
        });
        
        data.recordCheck(isAvailable, responseTimeMs);
        
        // Check for degradation
        if (detectDegradation(data)) {
            logger.warn("Availability degradation detected for service: {}, current: {}%, previous: {}%",
                serviceName, 
                data.getCurrentAvailability() * 100,
                data.getPreviousAvailability() * 100);
        }
    }

    /**
     * Calculates current availability percentage for a service.
     * 
     * @param serviceName The service component name
     * @return Availability percentage (0.0 to 1.0) or null if not tracked
     */
    public Double calculateCurrentAvailability(String serviceName) {
        ServiceAvailabilityData data = availabilityData.get(serviceName);
        return data != null ? data.getCurrentAvailability() : null;
    }

    /**
     * Calculates availability for a specific time period.
     * 
     * @param serviceName The service component name
     * @param startTime Start of the period
     * @param endTime End of the period
     * @return Availability percentage for the period
     */
    public double calculateAvailabilityForPeriod(String serviceName, Instant startTime, Instant endTime) {
        ServiceAvailabilityData data = availabilityData.get(serviceName);
        if (data == null) {
            return 1.0; // Assume available if no data
        }
        
        return data.calculateAvailabilityForPeriod(startTime, endTime);
    }

    /**
     * Gets historical availability data for a service.
     * 
     * @param serviceName The service component name
     * @param days Number of days of history to retrieve
     * @return List of daily availability percentages
     */
    public List<DailyAvailability> getHistoricalAvailability(String serviceName, int days) {
        ServiceAvailabilityData data = availabilityData.get(serviceName);
        if (data == null) {
            return List.of();
        }
        
        return data.getHistoricalData(days);
    }

    /**
     * Detects availability trend degradation.
     * 
     * @param serviceName The service component name
     * @return true if degradation is detected
     */
    public boolean detectDegradation(String serviceName) {
        ServiceAvailabilityData data = availabilityData.get(serviceName);
        return data != null && detectDegradation(data);
    }

    /**
     * Gets availability trend analysis.
     * 
     * @param serviceName The service component name
     * @return Trend analysis or null if not enough data
     */
    public AvailabilityTrend getAvailabilityTrend(String serviceName) {
        ServiceAvailabilityData data = availabilityData.get(serviceName);
        if (data == null) {
            return null;
        }
        
        return data.calculateTrend();
    }

    /**
     * Gets comprehensive availability report for a service.
     * 
     * @param serviceName The service component name
     * @return Availability report or null if not tracked
     */
    public AvailabilityReport getAvailabilityReport(String serviceName) {
        ServiceAvailabilityData data = availabilityData.get(serviceName);
        if (data == null) {
            return null;
        }
        
        return new AvailabilityReport(
            serviceName,
            data.getCurrentAvailability(),
            data.getUptimeSeconds(),
            data.getDowntimeSeconds(),
            data.getTotalChecks(),
            data.getLastDowntime(),
            data.getLastUptimeStart(),
            data.getMeanTimeBetweenFailures(),
            data.getMeanTimeToRecovery(),
            data.calculateTrend()
        );
    }

    /**
     * Scheduled cleanup of old historical data.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    public void cleanupHistoricalData() {
        Instant cutoffTime = Instant.now().minus(HISTORICAL_DATA_RETENTION_DAYS, ChronoUnit.DAYS);
        
        for (ServiceAvailabilityData data : availabilityData.values()) {
            data.cleanupOldData(cutoffTime);
        }
        
        logger.info("Cleaned up availability historical data older than {} days", 
            HISTORICAL_DATA_RETENTION_DAYS);
    }

    /**
     * Detects degradation in availability data.
     */
    private boolean detectDegradation(ServiceAvailabilityData data) {
        double current = data.getCurrentAvailability();
        double previous = data.getPreviousAvailability();
        
        return (previous - current) > DEGRADATION_THRESHOLD;
    }

    /**
     * Registers availability metrics with the meter registry.
     */
    private void registerAvailabilityMetrics(ServiceAvailabilityData data) {
        Gauge.builder("service.availability.current", data, d -> d.getCurrentAvailability() * 100)
            .description("Current service availability percentage")
            .tags("service", data.serviceName)
            .register(meterRegistry);
            
        Gauge.builder("service.availability.uptime.seconds", data, ServiceAvailabilityData::getUptimeSeconds)
            .description("Total uptime in seconds")
            .tags("service", data.serviceName)
            .register(meterRegistry);
            
        Gauge.builder("service.availability.downtime.seconds", data, ServiceAvailabilityData::getDowntimeSeconds)
            .description("Total downtime in seconds")
            .tags("service", data.serviceName)
            .register(meterRegistry);
            
        Gauge.builder("service.availability.mtbf.seconds", data, ServiceAvailabilityData::getMeanTimeBetweenFailures)
            .description("Mean time between failures")
            .tags("service", data.serviceName)
            .register(meterRegistry);
            
        Gauge.builder("service.availability.mttr.seconds", data, ServiceAvailabilityData::getMeanTimeToRecovery)
            .description("Mean time to recovery")
            .tags("service", data.serviceName)
            .register(meterRegistry);
    }

    // Inner classes
    
    private static class ServiceAvailabilityData {
        private final String serviceName;
        private final List<AvailabilityCheckRecord> checks = new ArrayList<>();
        private final List<DailyAvailability> dailyHistory = new ArrayList<>();
        
        private long totalChecks = 0;
        private long availableChecks = 0;
        private long uptimeSeconds = 0;
        private long downtimeSeconds = 0;
        private Instant lastCheckTime = Instant.now();
        private boolean wasAvailable = true;
        private Instant lastDowntime = null;
        private Instant lastUptimeStart = Instant.now();
        
        private final List<Instant> failureTimes = new ArrayList<>();
        private final List<Duration> recoveryDurations = new ArrayList<>();
        
        public ServiceAvailabilityData(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public synchronized void recordCheck(boolean isAvailable, long responseTimeMs) {
            Instant now = Instant.now();
            long secondsSinceLastCheck = Duration.between(lastCheckTime, now).getSeconds();
            
            // Update uptime/downtime
            if (wasAvailable) {
                uptimeSeconds += secondsSinceLastCheck;
            } else {
                downtimeSeconds += secondsSinceLastCheck;
            }
            
            // Track state transitions
            if (wasAvailable && !isAvailable) {
                // Service went down
                lastDowntime = now;
                failureTimes.add(now);
            } else if (!wasAvailable && isAvailable) {
                // Service recovered
                lastUptimeStart = now;
                if (lastDowntime != null) {
                    Duration recoveryTime = Duration.between(lastDowntime, now);
                    recoveryDurations.add(recoveryTime);
                }
            }
            
            // Record check
            totalChecks++;
            if (isAvailable) {
                availableChecks++;
            }
            
            checks.add(new AvailabilityCheckRecord(now, isAvailable, responseTimeMs));
            
            // Update daily history
            updateDailyHistory(now, isAvailable);
            
            lastCheckTime = now;
            wasAvailable = isAvailable;
        }
        
        public double getCurrentAvailability() {
            if (totalChecks == 0) return 1.0;
            return (double) availableChecks / totalChecks;
        }
        
        public double getPreviousAvailability() {
            // Calculate availability for the previous 24-hour window
            Instant now = Instant.now();
            Instant windowStart = now.minus(48, ChronoUnit.HOURS);
            Instant windowEnd = now.minus(24, ChronoUnit.HOURS);
            
            return calculateAvailabilityForPeriod(windowStart, windowEnd);
        }
        
        public double calculateAvailabilityForPeriod(Instant startTime, Instant endTime) {
            long totalChecksInPeriod = 0;
            long availableChecksInPeriod = 0;
            
            for (AvailabilityCheckRecord check : checks) {
                if (check.timestamp.isAfter(startTime) && check.timestamp.isBefore(endTime)) {
                    totalChecksInPeriod++;
                    if (check.isAvailable) {
                        availableChecksInPeriod++;
                    }
                }
            }
            
            if (totalChecksInPeriod == 0) return 1.0;
            return (double) availableChecksInPeriod / totalChecksInPeriod;
        }
        
        public List<DailyAvailability> getHistoricalData(int days) {
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
            return dailyHistory.stream()
                .filter(daily -> daily.date.isAfter(cutoff))
                .toList();
        }
        
        public AvailabilityTrend calculateTrend() {
            if (dailyHistory.size() < 2) {
                return new AvailabilityTrend(TrendDirection.STABLE, 0.0, "Insufficient data");
            }
            
            // Calculate trend over the last 7 days
            int windowSize = Math.min(7, dailyHistory.size());
            List<DailyAvailability> recentDays = dailyHistory.subList(
                dailyHistory.size() - windowSize, 
                dailyHistory.size()
            );
            
            double firstAvg = recentDays.subList(0, windowSize / 2).stream()
                .mapToDouble(DailyAvailability::availabilityPercentage)
                .average()
                .orElse(1.0);
                
            double secondAvg = recentDays.subList(windowSize / 2, windowSize).stream()
                .mapToDouble(DailyAvailability::availabilityPercentage)
                .average()
                .orElse(1.0);
            
            double change = secondAvg - firstAvg;
            
            TrendDirection direction;
            String description;
            
            if (Math.abs(change) < DEGRADATION_THRESHOLD) {
                direction = TrendDirection.STABLE;
                description = "Availability is stable";
            } else if (change > 0) {
                direction = TrendDirection.IMPROVING;
                description = String.format("Availability improving by %.2f%%", change * 100);
            } else {
                direction = TrendDirection.DEGRADING;
                description = String.format("Availability degrading by %.2f%%", Math.abs(change) * 100);
            }
            
            return new AvailabilityTrend(direction, change, description);
        }
        
        public long getUptimeSeconds() {
            return uptimeSeconds;
        }
        
        public long getDowntimeSeconds() {
            return downtimeSeconds;
        }
        
        public long getTotalChecks() {
            return totalChecks;
        }
        
        public Instant getLastDowntime() {
            return lastDowntime;
        }
        
        public Instant getLastUptimeStart() {
            return lastUptimeStart;
        }
        
        public double getMeanTimeBetweenFailures() {
            if (failureTimes.size() < 2) return 0.0;
            
            long totalTimeBetweenFailures = 0;
            for (int i = 1; i < failureTimes.size(); i++) {
                totalTimeBetweenFailures += Duration.between(
                    failureTimes.get(i - 1), 
                    failureTimes.get(i)
                ).getSeconds();
            }
            
            return (double) totalTimeBetweenFailures / (failureTimes.size() - 1);
        }
        
        public double getMeanTimeToRecovery() {
            if (recoveryDurations.isEmpty()) return 0.0;
            
            long totalRecoveryTime = recoveryDurations.stream()
                .mapToLong(Duration::getSeconds)
                .sum();
                
            return (double) totalRecoveryTime / recoveryDurations.size();
        }
        
        private void updateDailyHistory(Instant checkTime, boolean isAvailable) {
            LocalDateTime checkDate = LocalDateTime.ofInstant(checkTime, ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.DAYS);
            Instant dayStart = checkDate.toInstant(ZoneOffset.UTC);
            
            // Find or create daily record
            DailyAvailability dailyRecord = dailyHistory.stream()
                .filter(d -> d.date.equals(dayStart))
                .findFirst()
                .orElse(null);
                
            if (dailyRecord == null) {
                dailyRecord = new DailyAvailability(dayStart, 0, 0);
                dailyHistory.add(dailyRecord);
            }
            
            // Update daily record
            dailyRecord.totalChecks++;
            if (isAvailable) {
                dailyRecord.availableChecks++;
            }
        }
        
        public void cleanupOldData(Instant cutoffTime) {
            checks.removeIf(check -> check.timestamp.isBefore(cutoffTime));
            dailyHistory.removeIf(daily -> daily.date.isBefore(cutoffTime));
            failureTimes.removeIf(time -> time.isBefore(cutoffTime));
        }
    }
    
    private record AvailabilityCheckRecord(
        Instant timestamp,
        boolean isAvailable,
        long responseTimeMs
    ) {}
    
    public static class DailyAvailability {
        private final Instant date;
        private int totalChecks;
        private int availableChecks;
        
        public DailyAvailability(Instant date, int totalChecks, int availableChecks) {
            this.date = date;
            this.totalChecks = totalChecks;
            this.availableChecks = availableChecks;
        }
        
        public Instant getDate() {
            return date;
        }
        
        public double availabilityPercentage() {
            if (totalChecks == 0) return 1.0;
            return (double) availableChecks / totalChecks;
        }
        
        public int getTotalChecks() {
            return totalChecks;
        }
        
        public int getAvailableChecks() {
            return availableChecks;
        }
    }
    
    public enum TrendDirection {
        IMPROVING,
        STABLE,
        DEGRADING
    }
    
    public record AvailabilityTrend(
        TrendDirection direction,
        double changePercentage,
        String description
    ) {}
    
    public record AvailabilityReport(
        String serviceName,
        double currentAvailability,
        long uptimeSeconds,
        long downtimeSeconds,
        long totalChecks,
        Instant lastDowntime,
        Instant lastUptimeStart,
        double meanTimeBetweenFailures,
        double meanTimeToRecovery,
        AvailabilityTrend trend
    ) {}
}
