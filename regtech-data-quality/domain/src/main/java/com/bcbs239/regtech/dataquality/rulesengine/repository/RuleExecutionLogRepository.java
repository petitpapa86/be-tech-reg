package com.bcbs239.regtech.dataquality.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLog;

/**
 * Domain repository interface for RuleExecutionLog persistence operations.
 * This interface defines the contract for storing and retrieving rule execution logs.
 */
public interface RuleExecutionLogRepository {
    
    /**
     * Saves a rule execution log to the persistence store.
     * 
     * @param executionLog The rule execution log to save
     * @return The saved rule execution log with generated ID
     */
    RuleExecutionLog save(RuleExecutionLog executionLog);
}
