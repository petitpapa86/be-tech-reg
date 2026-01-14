package com.bcbs239.regtech.iam.domain.bankprofile;

import com.bcbs239.regtech.core.domain.shared.Maybe;

/**
 * Repository interface (domain layer)
 * Implementation in infrastructure layer
 */
public interface BankProfileRepository {
    
    /**
     * Find bank profile by ID
     */
    Maybe<BankProfile> findById(String bankId);
    
    /**
     * Save/update bank profile
     */
    BankProfile save(BankProfile profile);
}
