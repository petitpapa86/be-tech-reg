package com.bcbs239.regtech.ingestion.domain.model;

import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

import java.util.Set;

/**
 * Utility class for validating batch status transitions.
 * Encapsulates the business rules for valid state changes.
 */
public class BatchTransitions {
    
    /**
     * Validates if a transition from one status to another is allowed.
     */
    public static Result<Void> validateTransition(BatchStatus from, BatchStatus to) {
        if (from == null) {
            return Result.failure(new ErrorDetail("INVALID_FROM_STATUS", "From status cannot be null"));
        }
        
        if (to == null) {
            return Result.failure(new ErrorDetail("INVALID_TO_STATUS", "To status cannot be null"));
        }
        
        if (from == to) {
            return Result.failure(new ErrorDetail("SAME_STATUS_TRANSITION", 
                String.format("Cannot transition from %s to the same status", from)));
        }
        
        Set<BatchStatus> validTransitions = getValidTransitions(from);
        
        if (!validTransitions.contains(to)) {
            return Result.failure(new ErrorDetail("INVALID_STATE_TRANSITION", 
                String.format("Cannot transition from %s to %s. Valid transitions are: %s", 
                    from, to, validTransitions)));
        }
        
        return Result.success(null);
    }
    
    /**
     * Gets all valid transitions from a given status.
     */
    public static Set<BatchStatus> getValidTransitions(BatchStatus from) {
        return switch (from) {
            case UPLOADED -> Set.of(BatchStatus.PARSING, BatchStatus.FAILED);
            case PARSING -> Set.of(BatchStatus.VALIDATED, BatchStatus.FAILED);
            case VALIDATED -> Set.of(BatchStatus.STORING, BatchStatus.FAILED);
            case STORING -> Set.of(BatchStatus.COMPLETED, BatchStatus.FAILED);
            case COMPLETED, FAILED -> Set.of(); // Terminal states - no transitions allowed
        };
    }
    
    /**
     * Checks if a status is a terminal state (no further transitions allowed).
     */
    public static boolean isTerminalState(BatchStatus status) {
        return status == BatchStatus.COMPLETED || status == BatchStatus.FAILED;
    }
    
    /**
     * Checks if a transition is valid without returning a Result.
     */
    public static boolean isValidTransition(BatchStatus from, BatchStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        
        return getValidTransitions(from).contains(to);
    }
    
    /**
     * Gets the next expected status in the normal processing flow.
     */
    public static BatchStatus getNextExpectedStatus(BatchStatus current) {
        return switch (current) {
            case UPLOADED -> BatchStatus.PARSING;
            case PARSING -> BatchStatus.VALIDATED;
            case VALIDATED -> BatchStatus.STORING;
            case STORING -> BatchStatus.COMPLETED;
            case COMPLETED, FAILED -> null; // Terminal states
        };
    }
    
    /**
     * Checks if a status represents a successful processing state.
     */
    public static boolean isSuccessfulState(BatchStatus status) {
        return status == BatchStatus.COMPLETED;
    }
    
    /**
     * Checks if a status represents a failed processing state.
     */
    public static boolean isFailedState(BatchStatus status) {
        return status == BatchStatus.FAILED;
    }
    
    /**
     * Checks if a status represents an in-progress processing state.
     */
    public static boolean isInProgressState(BatchStatus status) {
        return status == BatchStatus.PARSING 
            || status == BatchStatus.VALIDATED 
            || status == BatchStatus.STORING;
    }
    
    /**
     * Gets a human-readable description of the status.
     */
    public static String getStatusDescription(BatchStatus status) {
        return switch (status) {
            case UPLOADED -> "File uploaded and awaiting processing";
            case PARSING -> "File is being parsed and validated";
            case VALIDATED -> "File has been successfully validated";
            case STORING -> "File is being stored in S3";
            case COMPLETED -> "Batch processing completed successfully";
            case FAILED -> "Batch processing failed";
        };
    }
}