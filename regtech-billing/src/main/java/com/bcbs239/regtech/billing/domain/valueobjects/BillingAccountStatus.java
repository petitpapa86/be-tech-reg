package com.bcbs239.regtech.billing.domain.valueobjects;

/**
 * Status enumeration for BillingAccount aggregate
 */
public enum BillingAccountStatus {
    /**
     * Account created but payment not yet verified
     */
    PENDING_VERIFICATION,
    
    /**
     * Account is active and in good standing
     */
    ACTIVE,
    
    /**
     * Account has overdue payments
     */
    PAST_DUE,
    
    /**
     * Account is temporarily suspended
     */
    SUSPENDED,
    
    /**
     * Account is permanently cancelled
     */
    CANCELLED;
    
    /**
     * Check if account can create new subscriptions
     */
    public boolean canCreateSubscription() {
        return this == ACTIVE;
    }
    
    /**
     * Check if account is in good standing
     */
    public boolean isInGoodStanding() {
        return this == ACTIVE;
    }
    
    /**
     * Check if account requires payment attention
     */
    public boolean requiresPaymentAttention() {
        return this == PAST_DUE || this == SUSPENDED;
    }
    
    /**
     * Check if account is terminated
     */
    public boolean isTerminated() {
        return this == CANCELLED;
    }
}