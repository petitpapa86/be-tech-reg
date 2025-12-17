package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLogDto;

import java.util.List;

/**
 * Port for rule execution log persistence.
 */
public interface RuleExecutionLogRepository {

    /**
     * Save a single execution log.
     */
    void save(RuleExecutionLogDto executionLog);

    /**
     * Save all execution logs for a batch.
     */
    void saveAllForBatch(String batchId, List<RuleExecutionLogDto> executionLogs);

    /**
     * Flush pending changes.
     */
    void flush();
}