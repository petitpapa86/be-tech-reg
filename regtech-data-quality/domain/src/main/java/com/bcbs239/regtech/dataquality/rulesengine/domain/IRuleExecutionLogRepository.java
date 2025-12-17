package com.bcbs239.regtech.dataquality.rulesengine.domain;

/**
 * Domain repository interface for rule execution logs.
 */
public interface IRuleExecutionLogRepository {
    
    /**
     * Saves a rule execution log.
     */
    void save(RuleExecutionLogDto executionLog);

    /**
     * Saves rule execution logs in batch.
     * Implementations backed by JPA should prefer bulk inserts.
     */
    default void saveAll(Iterable<RuleExecutionLogDto> executionLogs) {
        if (executionLogs == null) {
            return;
        }
        for (RuleExecutionLogDto executionLog : executionLogs) {
            save(executionLog);
        }
    }

    /**
     * Flushes pending writes (optional).
     */
    default void flush() {
        // no-op by default
    }
}
