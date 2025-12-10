package com.bcbs239.regtech.app.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SLA Monitoring Service for tracking service level objectives and performance metrics.
 * 
 * This service tracks:
 * - API response times against SLA thresholds
 * - Batch processing completion times against processing windows
 * - Service availability and uptime percentages
 * - Transaction volumes against capacity targets
 * - SLA breaches with impact assessment
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
@Service
public class SLAMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(SLAMonitoringService.class);

    private final MeterRegistry meterRegistry;
    
    // SLA thresholds (configurable via properties in production)
    private static final long API_RESPONSE_TIME_THRESHOLD_MS = 1000; // 1 second
    private static final long BATCH_PROCESSING_WINDOW_MS = 300000; // 5 minutes
    private static final double AVAILABILITY_TARGET = 0.999; // 99.9%
    private static final long THROUGHPUT_CAPACITY_TARGET = 10000; // transactions per hour
    
    // Tracking maps
    private final Map<String, ApiPerformanceTracker> apiTrackers = new ConcurrentHashMap<>();
    private final Map<String, BatchProcessingTracker> batchTrackers = new ConcurrentHashMap<>();
    private final Map<String, ServiceAvailabilityTracker> availabilityTrackers = new ConcurrentHashMap<>();
    private final Map<String, ThroughputTracker> throughputTrackers = new ConcurrentHashMap<>();
    
    // Counters for SLA breaches
    private final Counter apiSlaBreachCounter;
    private final Counter batchSlaBreachCounter;
    private final Counter availabilitySlaBreachCounter;
    private final Counter throughputSlaBreachCounter;

    public SLAMonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize SLA breach counters
        this.apiSlaBreachCounter = Counter.builder("sla.breach.api")
            .description("Number of API SLA breaches")
            .tag("sla.type", "response_time")
            .register(meterRegistry);
            
        this.batchSlaBreachCounter = Counter.builder("sla.breach.batch")
            .description("Number of batch processing SLA breaches")
            .tag("sla.type", "processing_window")
            .register(meterRegistry);
            
        this.availabilitySlaBreachCounter = Counter.builder("sla.breach.availability")
            .description("Number of availability SLA breaches")
            .tag("sla.type", "uptime")
            .register(meterRegistry);
            
        this.throughputSlaBreachCounter = Counter.builder("sla.breach.throughput")
            .description("Number of throughput SLA breaches")
            .tag("sla.type", "capacity")
            .register(meterRegistry);
    }

    /**
     * Records API response time and checks against SLA threshold.
     * 
     * @param endpoint The API endpoint
     * @param responseTimeMs Response time in milliseconds
     * @param statusCode HTTP status code
     * @return true if SLA was met, false if breached
     */
    public boolean recordApiResponseTime(String endpoint, long responseTimeMs, int statusCode) {
        ApiPerformanceTracker tracker = apiTrackers.computeIfAbsent(endpoint, key -> {
            ApiPerformanceTracker newTracker = new ApiPerformanceTracker(endpoint);
            registerApiMetrics(newTracker);
            return newTracker;
        });
        
        tracker.recordRequest(responseTimeMs, statusCode);
        
        boolean slaMet = responseTimeMs <= API_RESPONSE_TIME_THRESHOLD_MS;
        
        if (!slaMet) {
            apiSlaBreachCounter.increment();
            logger.warn("API SLA breach detected: endpoint={}, responseTime={}ms, threshold={}ms",
                endpoint, responseTimeMs, API_RESPONSE_TIME_THRESHOLD_MS);
            
            // Record breach details
            recordSLABreach("api", endpoint, responseTimeMs, API_RESPONSE_TIME_THRESHOLD_MS,
                "Response time exceeded threshold");
        }
        
        return slaMet;
    }

    /**
     * Records batch processing completion time and checks against processing window.
     * 
     * @param batchId The batch identifier
     * @param processingTimeMs Processing time in milliseconds
     * @param recordCount Number of records processed
     * @param success Whether processing was successful
     * @return true if SLA was met, false if breached
     */
    public boolean recordBatchProcessingTime(String batchId, long processingTimeMs, 
                                            int recordCount, boolean success) {
        BatchProcessingTracker tracker = batchTrackers.computeIfAbsent(batchId, key -> {
            BatchProcessingTracker newTracker = new BatchProcessingTracker(batchId);
            registerBatchMetrics(newTracker);
            return newTracker;
        });
        
        tracker.recordProcessing(processingTimeMs, recordCount, success);
        
        boolean slaMet = processingTimeMs <= BATCH_PROCESSING_WINDOW_MS;
        
        if (!slaMet) {
            batchSlaBreachCounter.increment();
            logger.warn("Batch processing SLA breach detected: batchId={}, processingTime={}ms, window={}ms",
                batchId, processingTimeMs, BATCH_PROCESSING_WINDOW_MS);
            
            // Record breach details
            recordSLABreach("batch", batchId, processingTimeMs, BATCH_PROCESSING_WINDOW_MS,
                "Processing time exceeded window");
        }
        
        return slaMet;
    }

    /**
     * Records service availability status.
     * 
     * @param serviceName The service component name
     * @param isAvailable Whether the service is currently available
     */
    public void recordServiceAvailability(String serviceName, boolean isAvailable) {
        ServiceAvailabilityTracker tracker = availabilityTrackers.computeIfAbsent(serviceName, key -> {
            ServiceAvailabilityTracker newTracker = new ServiceAvailabilityTracker(serviceName);
            registerAvailabilityMetrics(newTracker);
            return newTracker;
        });
        
        tracker.recordAvailability(isAvailable);
        
        double currentAvailability = tracker.getAvailabilityPercentage();
        
        if (currentAvailability < AVAILABILITY_TARGET) {
            availabilitySlaBreachCounter.increment();
            logger.warn("Availability SLA breach detected: service={}, availability={}%, target={}%",
                serviceName, currentAvailability * 100, AVAILABILITY_TARGET * 100);
            
            // Record breach details
            recordSLABreach("availability", serviceName, 
                (long) (currentAvailability * 100), 
                (long) (AVAILABILITY_TARGET * 100),
                "Availability below target");
        }
    }

    /**
     * Records transaction throughput.
     * 
     * @param component The component name
     * @param transactionCount Number of transactions in the current period
     */
    public void recordThroughput(String component, long transactionCount) {
        ThroughputTracker tracker = throughputTrackers.computeIfAbsent(component, key -> {
            ThroughputTracker newTracker = new ThroughputTracker(component);
            registerThroughputMetrics(newTracker);
            return newTracker;
        });
        
        tracker.recordTransactions(transactionCount);
        
        long currentThroughput = tracker.getCurrentThroughput();
        
        if (currentThroughput > THROUGHPUT_CAPACITY_TARGET) {
            throughputSlaBreachCounter.increment();
            logger.warn("Throughput capacity SLA breach detected: component={}, throughput={}, capacity={}",
                component, currentThroughput, THROUGHPUT_CAPACITY_TARGET);
            
            // Record breach details
            recordSLABreach("throughput", component, currentThroughput, THROUGHPUT_CAPACITY_TARGET,
                "Throughput exceeded capacity target");
        }
    }

    /**
     * Gets API performance metrics for a specific endpoint.
     * 
     * @param endpoint The API endpoint
     * @return Performance metrics or null if not tracked
     */
    public ApiPerformanceMetrics getApiPerformanceMetrics(String endpoint) {
        ApiPerformanceTracker tracker = apiTrackers.get(endpoint);
        if (tracker == null) {
            return null;
        }
        
        return new ApiPerformanceMetrics(
            endpoint,
            tracker.getAverageResponseTime(),
            tracker.getP95ResponseTime(),
            tracker.getP99ResponseTime(),
            tracker.getTotalRequests(),
            tracker.getErrorCount(),
            tracker.getSlaCompliancePercentage()
        );
    }

    /**
     * Gets batch processing metrics for a specific batch.
     * 
     * @param batchId The batch identifier
     * @return Processing metrics or null if not tracked
     */
    public BatchProcessingMetrics getBatchProcessingMetrics(String batchId) {
        BatchProcessingTracker tracker = batchTrackers.get(batchId);
        if (tracker == null) {
            return null;
        }
        
        return new BatchProcessingMetrics(
            batchId,
            tracker.getAverageProcessingTime(),
            tracker.getTotalRecordsProcessed(),
            tracker.getSuccessCount(),
            tracker.getFailureCount(),
            tracker.getSlaCompliancePercentage()
        );
    }

    /**
     * Gets service availability metrics.
     * 
     * @param serviceName The service component name
     * @return Availability metrics or null if not tracked
     */
    public ServiceAvailabilityMetrics getServiceAvailabilityMetrics(String serviceName) {
        ServiceAvailabilityTracker tracker = availabilityTrackers.get(serviceName);
        if (tracker == null) {
            return null;
        }
        
        return new ServiceAvailabilityMetrics(
            serviceName,
            tracker.getAvailabilityPercentage(),
            tracker.getUptimeSeconds(),
            tracker.getDowntimeSeconds(),
            tracker.getTotalChecks(),
            tracker.getLastDowntime()
        );
    }

    /**
     * Gets throughput metrics for a component.
     * 
     * @param component The component name
     * @return Throughput metrics or null if not tracked
     */
    public ThroughputMetrics getThroughputMetrics(String component) {
        ThroughputTracker tracker = throughputTrackers.get(component);
        if (tracker == null) {
            return null;
        }
        
        return new ThroughputMetrics(
            component,
            tracker.getCurrentThroughput(),
            tracker.getPeakThroughput(),
            tracker.getAverageThroughput(),
            tracker.getCapacityUtilization()
        );
    }

    /**
     * Records an SLA breach with detailed context.
     */
    private void recordSLABreach(String slaType, String identifier, long actualValue, 
                                long thresholdValue, String reason) {
        Counter.builder("sla.breach.details")
            .description("Detailed SLA breach information")
            .tag("sla.type", slaType)
            .tag("identifier", identifier)
            .tag("reason", reason)
            .register(meterRegistry)
            .increment();
            
        // Record breach severity based on how much threshold was exceeded
        double breachSeverity = (double) actualValue / thresholdValue;
        Gauge.builder("sla.breach.severity", breachSeverity, Number::doubleValue)
            .description("SLA breach severity ratio")
            .tags("sla.type", slaType, "identifier", identifier)
            .register(meterRegistry);
    }

    /**
     * Registers API performance metrics with the meter registry.
     */
    private void registerApiMetrics(ApiPerformanceTracker tracker) {
        Gauge.builder("sla.api.response_time.average", tracker, ApiPerformanceTracker::getAverageResponseTime)
            .description("Average API response time")
            .tags("endpoint", tracker.endpoint)
            .register(meterRegistry);
            
        Gauge.builder("sla.api.compliance.percentage", tracker, ApiPerformanceTracker::getSlaCompliancePercentage)
            .description("API SLA compliance percentage")
            .tags("endpoint", tracker.endpoint)
            .register(meterRegistry);
    }

    /**
     * Registers batch processing metrics with the meter registry.
     */
    private void registerBatchMetrics(BatchProcessingTracker tracker) {
        Gauge.builder("sla.batch.processing_time.average", tracker, BatchProcessingTracker::getAverageProcessingTime)
            .description("Average batch processing time")
            .tags("batch.id", tracker.batchId)
            .register(meterRegistry);
            
        Gauge.builder("sla.batch.compliance.percentage", tracker, BatchProcessingTracker::getSlaCompliancePercentage)
            .description("Batch processing SLA compliance percentage")
            .tags("batch.id", tracker.batchId)
            .register(meterRegistry);
    }

    /**
     * Registers availability metrics with the meter registry.
     */
    private void registerAvailabilityMetrics(ServiceAvailabilityTracker tracker) {
        Gauge.builder("sla.availability.percentage", tracker, ServiceAvailabilityTracker::getAvailabilityPercentage)
            .description("Service availability percentage")
            .tags("service", tracker.serviceName)
            .register(meterRegistry);
            
        Gauge.builder("sla.availability.uptime.seconds", tracker, ServiceAvailabilityTracker::getUptimeSeconds)
            .description("Service uptime in seconds")
            .tags("service", tracker.serviceName)
            .register(meterRegistry);
    }

    /**
     * Registers throughput metrics with the meter registry.
     */
    private void registerThroughputMetrics(ThroughputTracker tracker) {
        Gauge.builder("sla.throughput.current", tracker, ThroughputTracker::getCurrentThroughput)
            .description("Current throughput")
            .tags("component", tracker.component)
            .register(meterRegistry);
            
        Gauge.builder("sla.throughput.capacity_utilization", tracker, ThroughputTracker::getCapacityUtilization)
            .description("Capacity utilization percentage")
            .tags("component", tracker.component)
            .register(meterRegistry);
    }

    // Inner tracker classes
    
    private static class ApiPerformanceTracker {
        private final String endpoint;
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final AtomicLong slaBreachCount = new AtomicLong(0);
        
        public ApiPerformanceTracker(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public void recordRequest(long responseTimeMs, int statusCode) {
            totalRequests.incrementAndGet();
            totalResponseTime.addAndGet(responseTimeMs);
            
            if (statusCode >= 400) {
                errorCount.incrementAndGet();
            }
            
            if (responseTimeMs > API_RESPONSE_TIME_THRESHOLD_MS) {
                slaBreachCount.incrementAndGet();
            }
        }
        
        public double getAverageResponseTime() {
            long requests = totalRequests.get();
            return requests > 0 ? (double) totalResponseTime.get() / requests : 0.0;
        }
        
        public double getP95ResponseTime() {
            // Simplified - in production, use a histogram
            return getAverageResponseTime() * 1.5;
        }
        
        public double getP99ResponseTime() {
            // Simplified - in production, use a histogram
            return getAverageResponseTime() * 2.0;
        }
        
        public long getTotalRequests() {
            return totalRequests.get();
        }
        
        public long getErrorCount() {
            return errorCount.get();
        }
        
        public double getSlaCompliancePercentage() {
            long requests = totalRequests.get();
            if (requests == 0) return 100.0;
            long compliantRequests = requests - slaBreachCount.get();
            return (double) compliantRequests / requests * 100.0;
        }
    }
    
    private static class BatchProcessingTracker {
        private final String batchId;
        private final AtomicLong totalProcessingTime = new AtomicLong(0);
        private final AtomicLong totalRecordsProcessed = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
        private final AtomicLong slaBreachCount = new AtomicLong(0);
        private final AtomicLong totalBatches = new AtomicLong(0);
        
        public BatchProcessingTracker(String batchId) {
            this.batchId = batchId;
        }
        
        public void recordProcessing(long processingTimeMs, int recordCount, boolean success) {
            totalBatches.incrementAndGet();
            totalProcessingTime.addAndGet(processingTimeMs);
            totalRecordsProcessed.addAndGet(recordCount);
            
            if (success) {
                successCount.incrementAndGet();
            } else {
                failureCount.incrementAndGet();
            }
            
            if (processingTimeMs > BATCH_PROCESSING_WINDOW_MS) {
                slaBreachCount.incrementAndGet();
            }
        }
        
        public double getAverageProcessingTime() {
            long batches = totalBatches.get();
            return batches > 0 ? (double) totalProcessingTime.get() / batches : 0.0;
        }
        
        public long getTotalRecordsProcessed() {
            return totalRecordsProcessed.get();
        }
        
        public long getSuccessCount() {
            return successCount.get();
        }
        
        public long getFailureCount() {
            return failureCount.get();
        }
        
        public double getSlaCompliancePercentage() {
            long batches = totalBatches.get();
            if (batches == 0) return 100.0;
            long compliantBatches = batches - slaBreachCount.get();
            return (double) compliantBatches / batches * 100.0;
        }
    }
    
    private static class ServiceAvailabilityTracker {
        private final String serviceName;
        private final AtomicLong totalChecks = new AtomicLong(0);
        private final AtomicLong availableChecks = new AtomicLong(0);
        private final AtomicLong uptimeSeconds = new AtomicLong(0);
        private final AtomicLong downtimeSeconds = new AtomicLong(0);
        private Instant lastCheckTime = Instant.now();
        private boolean wasAvailable = true;
        private Instant lastDowntime = null;
        
        public ServiceAvailabilityTracker(String serviceName) {
            this.serviceName = serviceName;
        }
        
        public synchronized void recordAvailability(boolean isAvailable) {
            Instant now = Instant.now();
            long secondsSinceLastCheck = Duration.between(lastCheckTime, now).getSeconds();
            
            if (wasAvailable) {
                uptimeSeconds.addAndGet(secondsSinceLastCheck);
            } else {
                downtimeSeconds.addAndGet(secondsSinceLastCheck);
                lastDowntime = now;
            }
            
            totalChecks.incrementAndGet();
            if (isAvailable) {
                availableChecks.incrementAndGet();
            }
            
            lastCheckTime = now;
            wasAvailable = isAvailable;
        }
        
        public double getAvailabilityPercentage() {
            long checks = totalChecks.get();
            if (checks == 0) return 100.0;
            return (double) availableChecks.get() / checks;
        }
        
        public long getUptimeSeconds() {
            return uptimeSeconds.get();
        }
        
        public long getDowntimeSeconds() {
            return downtimeSeconds.get();
        }
        
        public long getTotalChecks() {
            return totalChecks.get();
        }
        
        public Instant getLastDowntime() {
            return lastDowntime;
        }
    }
    
    private static class ThroughputTracker {
        private final String component;
        private final AtomicLong currentThroughput = new AtomicLong(0);
        private final AtomicLong peakThroughput = new AtomicLong(0);
        private final AtomicLong totalTransactions = new AtomicLong(0);
        private final AtomicLong measurementCount = new AtomicLong(0);
        
        public ThroughputTracker(String component) {
            this.component = component;
        }
        
        public void recordTransactions(long transactionCount) {
            currentThroughput.set(transactionCount);
            totalTransactions.addAndGet(transactionCount);
            measurementCount.incrementAndGet();
            
            // Update peak if current exceeds it
            long current = currentThroughput.get();
            long peak = peakThroughput.get();
            if (current > peak) {
                peakThroughput.compareAndSet(peak, current);
            }
        }
        
        public long getCurrentThroughput() {
            return currentThroughput.get();
        }
        
        public long getPeakThroughput() {
            return peakThroughput.get();
        }
        
        public double getAverageThroughput() {
            long measurements = measurementCount.get();
            return measurements > 0 ? (double) totalTransactions.get() / measurements : 0.0;
        }
        
        public double getCapacityUtilization() {
            return (double) currentThroughput.get() / THROUGHPUT_CAPACITY_TARGET * 100.0;
        }
    }

    // Metrics DTOs
    
    public record ApiPerformanceMetrics(
        String endpoint,
        double averageResponseTime,
        double p95ResponseTime,
        double p99ResponseTime,
        long totalRequests,
        long errorCount,
        double slaCompliancePercentage
    ) {}
    
    public record BatchProcessingMetrics(
        String batchId,
        double averageProcessingTime,
        long totalRecordsProcessed,
        long successCount,
        long failureCount,
        double slaCompliancePercentage
    ) {}
    
    public record ServiceAvailabilityMetrics(
        String serviceName,
        double availabilityPercentage,
        long uptimeSeconds,
        long downtimeSeconds,
        long totalChecks,
        Instant lastDowntime
    ) {}
    
    public record ThroughputMetrics(
        String component,
        long currentThroughput,
        long peakThroughput,
        double averageThroughput,
        double capacityUtilization
    ) {}
}
