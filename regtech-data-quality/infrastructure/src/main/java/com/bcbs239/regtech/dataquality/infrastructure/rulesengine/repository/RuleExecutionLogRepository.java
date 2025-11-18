package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLog;
import com.bcbs239.regtech.dataquality.rulesengine.domain.ExecutionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for RuleExecutionLog entity operations.
 */
@Repository
public interface RuleExecutionLogRepository extends JpaRepository<RuleExecutionLog, Long> {
    
    /**
     * Finds all execution logs for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of execution logs
     */
    List<RuleExecutionLog> findByRuleIdOrderByExecutionTimestampDesc(String ruleId);
    
    /**
     * Finds all execution logs for a specific entity.
     * 
     * @param entityType The entity type
     * @param entityId The entity ID
     * @return List of execution logs
     */
    List<RuleExecutionLog> findByEntityTypeAndEntityIdOrderByExecutionTimestampDesc(
        String entityType, String entityId);
    
    /**
     * Finds execution logs within a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of execution logs
     */
    List<RuleExecutionLog> findByExecutionTimestampBetweenOrderByExecutionTimestampDesc(
        Instant startTime, Instant endTime);
    
    /**
     * Finds execution logs by result type.
     * 
     * @param result The execution result
     * @return List of execution logs
     */
    List<RuleExecutionLog> findByExecutionResultOrderByExecutionTimestampDesc(ExecutionResult result);
    
    /**
     * Finds recent execution logs for a rule.
     * 
     * @param ruleId The rule ID
     * @param limit Maximum number of results
     * @return List of recent execution logs
     */
    @Query(value = "SELECT e FROM RuleExecutionLog e WHERE e.ruleId = :ruleId " +
                   "ORDER BY e.executionTimestamp DESC LIMIT :limit")
    List<RuleExecutionLog> findRecentExecutions(@Param("ruleId") String ruleId, @Param("limit") int limit);
    
    /**
     * Counts executions with violations for a rule.
     * 
     * @param ruleId The rule ID
     * @return Number of executions with violations
     */
    @Query("SELECT COUNT(e) FROM RuleExecutionLog e WHERE e.ruleId = :ruleId AND e.violationCount > 0")
    long countExecutionsWithViolations(@Param("ruleId") String ruleId);
}
