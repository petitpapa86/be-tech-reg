package com.bcbs239.regtech.iam.domain.banks;

import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.BankId;

import java.util.List;

/**
 * IBankRepository - Domain repository interface for Bank aggregate operations
 * Requirements: 13.2, 13.5
 */
public interface IBankRepository {
    /**
     * Saves a bank
     */
    Result<BankId> save(Bank bank);
    
    /**
     * Finds a bank by ID
     */
    Maybe<Bank> findById(BankId id);
    
    /**
     * Finds all banks
     */
    List<Bank> findAll();
    
    /**
     * Finds banks by status
     */
    List<Bank> findByStatus(BankStatus status);
    
    /**
     * Checks if a bank exists by ID
     */
    boolean existsById(BankId id);
}
