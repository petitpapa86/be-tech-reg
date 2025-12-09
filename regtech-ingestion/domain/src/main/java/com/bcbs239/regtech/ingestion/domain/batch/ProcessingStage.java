package com.bcbs239.regtech.ingestion.domain.batch;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;

/**
 * Value object representing the user-friendly processing stage.
 * Encapsulates the mapping from BatchStatus to human-readable stage names.
 */
public record ProcessingStage(String value) {
    
    // Stage constants
    public static final ProcessingStage QUEUED = new ProcessingStage("Queued");
    public static final ProcessingStage PARSING = new ProcessingStage("Parsing");
    public static final ProcessingStage ENRICHING = new ProcessingStage("Enriching");
    public static final ProcessingStage STORING = new ProcessingStage("Storing");
    public static final ProcessingStage COMPLETED = new ProcessingStage("Completed");
    public static final ProcessingStage FAILED = new ProcessingStage("Failed");
    
    /**
     * Create ProcessingStage with validation.
     */
    public static Result<ProcessingStage> create(String stage) {
        if (stage == null || stage.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of(
                "INVALID_PROCESSING_STAGE",
                ErrorType.VALIDATION_ERROR,
                "Processing stage cannot be null or empty",
                "batch.stage.invalid"
            ));
        }
        return Result.success(new ProcessingStage(stage.trim()));
    }
    
    /**
     * Create ProcessingStage from BatchStatus.
     */
    public static ProcessingStage fromBatchStatus(BatchStatus status) {
        return switch (status) {
            case UPLOADED -> QUEUED;
            case PARSING -> PARSING;
            case VALIDATED -> ENRICHING;
            case STORING -> STORING;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
        };
    }
    
    /**
     * Check if the stage represents a terminal state.
     */
    public boolean isTerminal() {
        return this.equals(COMPLETED) || this.equals(FAILED);
    }
    
    /**
     * Check if the stage represents active processing.
     */
    public boolean isProcessing() {
        return this.equals(PARSING) || this.equals(ENRICHING) || this.equals(STORING);
    }
    
    /**
     * Check if the stage represents a successful completion.
     */
    public boolean isSuccessful() {
        return this.equals(COMPLETED);
    }
    
    /**
     * Check if the stage represents a failure.
     */
    public boolean isFailure() {
        return this.equals(FAILED);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
