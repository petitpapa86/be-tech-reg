package com.bcbs239.regtech.billing.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object representing a dunning action that was executed.
 * Immutable record of what action was taken and when during the dunning process.
 */
public record DunningAction(
    DunningStep step,
    Instant executedAt,
    String actionType,
    String details,
    boolean successful
) {
    
    public DunningAction {
        Objects.requireNonNull(step, "DunningStep cannot be null");
        Objects.requireNonNull(executedAt, "ExecutedAt timestamp cannot be null");
        Objects.requireNonNull(actionType, "ActionType cannot be null");
        
        if (actionType.trim().isEmpty()) {
            throw new IllegalArgumentException("ActionType cannot be empty");
        }
    }
    
    /**
     * Create a successful dunning action.
     * 
     * @param step The dunning step that was executed
     * @param actionType The type of action (e.g., "EMAIL_SENT", "ACCOUNT_SUSPENDED")
     * @param details Additional details about the action
     * @return DunningAction marked as successful
     */
    public static DunningAction successful(DunningStep step, String actionType, String details) {
        return new DunningAction(step, Instant.now(), actionType, details, true);
    }
    
    /**
     * Create a failed dunning action.
     * 
     * @param step The dunning step that was attempted
     * @param actionType The type of action that was attempted
     * @param details Error details or reason for failure
     * @return DunningAction marked as failed
     */
    public static DunningAction failed(DunningStep step, String actionType, String details) {
        return new DunningAction(step, Instant.now(), actionType, details, false);
    }
    
    /**
     * Create a dunning action with specific timestamp.
     * 
     * @param step The dunning step
     * @param executedAt When the action was executed
     * @param actionType The type of action
     * @param details Additional details
     * @param successful Whether the action was successful
     * @return DunningAction with specified parameters
     */
    public static DunningAction of(DunningStep step, Instant executedAt, String actionType, 
                                  String details, boolean successful) {
        return new DunningAction(step, executedAt, actionType, details, successful);
    }
    
    @Override
    public String toString() {
        return String.format("DunningAction{step=%s, actionType=%s, successful=%s, executedAt=%s}", 
            step, actionType, successful, executedAt);
    }
}