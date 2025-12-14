package com.bcbs239.regtech.riskcalculation.domain.shared;

import java.time.Instant;
import java.util.Map;

public interface IPerformanceMetrics {
    void recordBatchStart(String batchId);

    void recordBatchSuccess(String batchId, int exposureCount);

    void recordBatchFailure(String batchId, String errorMessage);

    MetricsSnapshot getSnapshot();

    void resetThroughputWindow();

    Long getBatchProcessingTime(String batchId);

    void cleanupOldBatchTimes(int keepLastN);

    /**
     * Snapshot of current metrics.
     */
    public record MetricsSnapshot(
            long totalBatchesProcessed,
            long totalBatchesFailed,
            long totalExposuresProcessed,
            double averageProcessingTimeMillis,
            double errorRatePercent,
            long activeCalculations,
            double throughputPerHour,
            double averageExposuresPerBatch,
            Instant timestamp
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "totalBatchesProcessed", totalBatchesProcessed,
                    "totalBatchesFailed", totalBatchesFailed,
                    "totalExposuresProcessed", totalExposuresProcessed,
                    "averageProcessingTimeMillis", averageProcessingTimeMillis,
                    "errorRatePercent", errorRatePercent,
                    "activeCalculations", activeCalculations,
                    "throughputPerHour", throughputPerHour,
                    "averageExposuresPerBatch", averageExposuresPerBatch,
                    "timestamp", timestamp.toString()
            );
        }
    }
}
