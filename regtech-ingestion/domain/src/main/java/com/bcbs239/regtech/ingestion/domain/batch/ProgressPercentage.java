package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Value object representing progress as a percentage (0-100).
 * Ensures valid percentage values and provides semantic meaning to progress tracking.
 */
public record ProgressPercentage(int value) {
    
    private static final int MIN_PERCENTAGE = 0;
    private static final int MAX_PERCENTAGE = 100;
    
    /**
     * Create a ProgressPercentage with validation.
     */
    public static Result<ProgressPercentage> create(int percentage) {
        if (percentage < MIN_PERCENTAGE || percentage > MAX_PERCENTAGE) {
            return Result.failure(ErrorDetail.of(
                "INVALID_PROGRESS_PERCENTAGE",
                ErrorType.VALIDATION_ERROR,
                "Progress percentage must be between 0 and 100, got: " + percentage,
                "batch.progress.invalid"
            ));
        }
        return Result.success(new ProgressPercentage(percentage));
    }
    
    /**
     * Create ProgressPercentage based on BatchStatus.
     * Encapsulates the business logic for mapping status to progress percentage.
     */
    public static ProgressPercentage fromBatchStatus(BatchStatus status) {
        return switch (status) {
            case UPLOADED -> new ProgressPercentage(10);    // 10% - File uploaded, queued for processing
            case PARSING -> new ProgressPercentage(30);     // 30% - Parsing in progress
            case VALIDATED -> new ProgressPercentage(60);   // 60% - Validated, enriching data
            case STORING -> new ProgressPercentage(80);     // 80% - Storing to database
            case COMPLETED -> completed();                   // 100% - Completed successfully
            case FAILED -> notStarted();                     // 0% - Failed, no progress
        };
    }
    
    /**
     * Create ProgressPercentage for a completed operation (100%).
     */
    public static ProgressPercentage completed() {
        return new ProgressPercentage(MAX_PERCENTAGE);
    }
    
    /**
     * Create ProgressPercentage for a not started operation (0%).
     */
    public static ProgressPercentage notStarted() {
        return new ProgressPercentage(MIN_PERCENTAGE);
    }
    
    /**
     * Check if the progress is complete.
     */
    public boolean isComplete() {
        return value == MAX_PERCENTAGE;
    }
    
    /**
     * Check if the progress has started.
     */
    public boolean hasStarted() {
        return value > MIN_PERCENTAGE;
    }
    
    /**
     * Get the remaining percentage.
     */
    public int remaining() {
        return MAX_PERCENTAGE - value;
    }
    
    /**
     * Get the progress as a decimal (0.0 to 1.0).
     */
    public double asDecimal() {
        return value / 100.0;
    }
    
    @Override
    public String toString() {
        return value + "%";
    }
}
