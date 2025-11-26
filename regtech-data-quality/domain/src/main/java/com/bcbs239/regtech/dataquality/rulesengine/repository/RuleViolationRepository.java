package com.bcbs239.regtech.dataquality.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

/**
 * Domain repository interface for RuleViolation persistence operations.
 * This interface defines the contract for storing and retrieving rule violations.
 */
public interface RuleViolationRepository {
    
    /**
     * Saves a rule violation to the persistence store.
     * 
     * @param violation The rule violation to save
     * @return The saved rule violation with generated ID
     */
    RuleViolation save(RuleViolation violation);
}
