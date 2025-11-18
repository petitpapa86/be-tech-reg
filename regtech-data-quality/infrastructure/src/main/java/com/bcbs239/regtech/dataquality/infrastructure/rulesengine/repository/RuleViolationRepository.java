package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;
import com.bcbs239.regtech.dataquality.rulesengine.domain.ResolutionStatus;
import com.bcbs239.regtech.dataquality.rulesengine.domain.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for RuleViolation entity operations.
 */
@Repository
public interface RuleViolationRepository extends JpaRepository<RuleViolation, Long> {
    
    /**
     * Finds all violations for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of violations
     */
    List<RuleViolation> findByRuleIdOrderByDetectedAtDesc(String ruleId);
    
    /**
     * Finds all violations for a specific entity.
     * 
     * @param entityType The entity type
     * @param entityId The entity ID
     * @return List of violations
     */
    List<RuleViolation> findByEntityTypeAndEntityIdOrderByDetectedAtDesc(
        String entityType, String entityId);
    
    /**
     * Finds all open violations.
     * 
     * @return List of open violations
     */
    List<RuleViolation> findByResolutionStatusOrderByDetectedAtDesc(ResolutionStatus status);
    
    /**
     * Finds violations by severity.
     * 
     * @param severity The severity level
     * @return List of violations
     */
    List<RuleViolation> findBySeverityOrderByDetectedAtDesc(Severity severity);
    
    /**
     * Finds open violations for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of open violations
     */
    @Query("SELECT v FROM RuleViolation v WHERE v.ruleId = :ruleId " +
           "AND v.resolutionStatus IN ('OPEN', 'IN_PROGRESS') " +
           "ORDER BY v.detectedAt DESC")
    List<RuleViolation> findOpenViolationsByRule(@Param("ruleId") String ruleId);
    
    /**
     * Counts open violations by severity.
     * 
     * @param severity The severity level
     * @return Number of open violations
     */
    @Query("SELECT COUNT(v) FROM RuleViolation v WHERE v.severity = :severity " +
           "AND v.resolutionStatus IN ('OPEN', 'IN_PROGRESS')")
    long countOpenViolationsBySeverity(@Param("severity") Severity severity);
    
    /**
     * Finds violations detected within a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of violations
     */
    List<RuleViolation> findByDetectedAtBetweenOrderByDetectedAtDesc(
        Instant startTime, Instant endTime);
}
