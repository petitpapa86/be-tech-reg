package com.bcbs239.regtech.billing.domain.valueobjects;

/**
 * Enumeration of dunning action types that can be executed during the dunning process.
 * Each type represents a specific action taken to collect overdue payments.
 */
public enum DunningActionType {
    
    /**
     * Email reminder sent to customer about overdue payment
     */
    EMAIL_REMINDER,
    
    /**
     * Automatic payment retry attempted
     */
    PAYMENT_RETRY,
    
    /**
     * Account suspended due to non-payment
     */
    ACCOUNT_SUSPENSION,
    
    /**
     * Collection notice sent to customer
     */
    COLLECTION_NOTICE;
    
    /**
     * Get a human-readable description of the action type.
     * 
     * @return String description of the action
     */
    public String getDescription() {
        return switch (this) {
            case EMAIL_REMINDER -> "Email reminder sent to customer";
            case PAYMENT_RETRY -> "Automatic payment retry attempted";
            case ACCOUNT_SUSPENSION -> "Account suspended due to non-payment";
            case COLLECTION_NOTICE -> "Collection notice sent to customer";
        };
    }
    
    /**
     * Check if this action type is a final action (irreversible).
     * 
     * @return true if this is a final action, false otherwise
     */
    public boolean isFinalAction() {
        return this == ACCOUNT_SUSPENSION;
    }
}