package com.bcbs239.regtech.billing.domain.subscriptions;

/**
 * Status enumeration for Subscription aggregate
 */
public enum SubscriptionStatus {
    /**
     * Subscription is active and billing normally
     */
    ACTIVE,
    
    /**
     * Subscription has past due payments
     */
    PAST_DUE,
    
    /**
     * Subscription is temporarily paused
     */
    PAUSED,
    
    /**
     * Subscription is cancelled and will not renew
     */
    CANCELLED;
    
    /**
     * Check if subscription is actively billing
     */
    public boolean isActiveBilling() {
        return this == ACTIVE;
    }
    
    /**
     * Check if subscription requires payment attention
     */
    public boolean requiresPaymentAttention() {
        return this == PAST_DUE;
    }
    
    /**
     * Check if subscription is terminated
     */
    public boolean isTerminated() {
        return this == CANCELLED;
    }
    
    /**
     * Check if subscription can be reactivated
     */
    public boolean canBeReactivated() {
        return this == PAST_DUE || this == PAUSED;
    }
}

