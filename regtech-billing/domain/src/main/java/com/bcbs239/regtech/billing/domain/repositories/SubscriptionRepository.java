package com.bcbs239.regtech.billing.domain.repositories;

import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;

/**
 * Domain repository interface for Subscription aggregate operations.
 * Clean interface with direct method signatures.
 */
public interface SubscriptionRepository {
    
    /**
     * Find a subscription by ID
     */
    Maybe<Subscription> findById(SubscriptionId subscriptionId);
    
    /**
     * Find active subscription by billing account ID
     */
    Maybe<Subscription> findActiveByBillingAccountId(BillingAccountId billingAccountId);
    
    /**
     * Save a subscription
     */
    Result<SubscriptionId> save(Subscription subscription);
}