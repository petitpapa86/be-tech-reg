package com.bcbs239.regtech.billing.domain.repositories;

import com.bcbs239.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.core.shared.Maybe;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;

/**
 * Domain repository interface for BillingAccount aggregate operations.
 * Clean interface with direct method signatures.
 */
public interface BillingAccountRepository {
    
    /**
     * Find a billing account by ID
     */
    Maybe<BillingAccount> findById(BillingAccountId billingAccountId);
    
    /**
     * Find a billing account by user ID
     */
    Maybe<BillingAccount> findByUserId(UserId userId);
    
    /**
     * Save a billing account
     */
    Result<BillingAccountId> save(BillingAccount billingAccount);
    
    /**
     * Update a billing account
     */
    Result<BillingAccountId> update(BillingAccount billingAccount);
}