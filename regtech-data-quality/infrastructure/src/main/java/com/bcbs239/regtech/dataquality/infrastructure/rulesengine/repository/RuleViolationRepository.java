package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.domain.rules.ResolutionStatus;
import com.bcbs239.regtech.dataquality.domain.rules.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for RuleViolationEntity operations.
 */
@Repository
public interface RuleViolationRepository extends JpaRepository<RuleViolationEntity, Long> {
    
    /**
     * Finds all violations for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of violations
     */
    List<RuleViolationEntity> findByRuleIdOrderByDetectedAtDesc(String ruleId);
    
    /**
     * Finds all violations for a specific entity.
     * 
     * @param entityType The entity type
     * @param entityId The entity ID
     * @return List of violations
     */
    List<RuleViolationEntity> findByEntityTypeAndEntityIdOrderByDetectedAtDesc(
        String entityType, String entityId);
    
    /**
     * Finds all open violations.
     * 
     * @return List of open violations
     */
    List<RuleViolationEntity> findByResolutionStatusOrderByDetectedAtDesc(ResolutionStatus status);
    
    /**
     * Finds violations by severity.
     * 
     * @param severity The severity level
     * @return List of violations
     */
    List<RuleViolationEntity> findBySeverityOrderByDetectedAtDesc(Severity severity);
    
    /**
     * Finds open violations for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of open violations
     */
    @Query("SELECT v FROM RuleViolationEntity v WHERE v.ruleId = :ruleId " +
           "AND v.resolutionStatus IN ('OPEN', 'IN_PROGRESS') " +
           "ORDER BY v.detectedAt DESC")
    List<RuleViolationEntity> findOpenViolationsByRule(@Param("ruleId") String ruleId);
    
    /**
     * Counts open violations by severity.
     * 
     * @param severity The severity level
     * @return Number of open violations
     */
    @Query("SELECT COUNT(v) FROM RuleViolationEntity v WHERE v.severity = :severity " +
           "AND v.resolutionStatus IN ('OPEN', 'IN_PROGRESS')")
    long countOpenViolationsBySeverity(@Param("severity") Severity severity);
    
    /**
     * Finds violations detected within a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return List of violations
     */
    List<RuleViolationEntity> findByDetectedAtBetweenOrderByDetectedAtDesc(
        Instant startTime, Instant endTime);
}
