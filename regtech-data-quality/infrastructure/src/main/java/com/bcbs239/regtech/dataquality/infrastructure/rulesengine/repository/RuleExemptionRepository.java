package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Infrastructure repository for managing rule exemptions.
 * Extends both JpaRepository for Spring Data operations and the domain repository interface.
 * 
 * <p>Provides queries for finding active exemptions that apply to specific
 * rules and entities.</p>
 */
@Repository
public interface RuleExemptionRepository extends JpaRepository<RuleExemption, Long>,
        com.bcbs239.regtech.dataquality.rulesengine.repository.RuleExemptionRepository {
    
    /**
     * Finds all exemptions for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of exemptions for the rule
     */
    @Query("SELECT e FROM RuleExemption e WHERE e.rule.ruleId = :ruleId")
    List<RuleExemption> findByRuleId(@Param("ruleId") String ruleId);
    
    /**
     * Finds active exemptions for a specific rule and entity.
     * An exemption is active if:
     * - Current date is on or after effective_date
     * - Current date is before expiration_date (or expiration_date is null)
     * - Entity type matches
     * - Entity ID matches (or entity_id is null for wildcard exemptions)
     * 
     * @param ruleId The rule ID
     * @param entityType The entity type (e.g., "EXPOSURE")
     * @param entityId The entity ID
     * @param currentDate The current date for checking validity
     * @return List of active exemptions
     */
    @Query("""
        SELECT e FROM RuleExemption e 
        WHERE e.rule.ruleId = :ruleId 
        AND e.entityType = :entityType 
        AND (e.entityId IS NULL OR e.entityId = :entityId)
        AND e.effectiveDate <= :currentDate
        AND (e.expirationDate IS NULL OR e.expirationDate >= :currentDate)
        """)
    List<RuleExemption> findActiveExemptions(
        @Param("ruleId") String ruleId,
        @Param("entityType") String entityType,
        @Param("entityId") String entityId,
        @Param("currentDate") LocalDate currentDate
    );
    
    /**
     * Finds all active exemptions for a specific rule.
     * 
     * @param ruleId The rule ID
     * @param currentDate The current date for checking validity
     * @return List of active exemptions
     */
    @Query("""
        SELECT e FROM RuleExemption e 
        WHERE e.rule.ruleId = :ruleId 
        AND e.effectiveDate <= :currentDate
        AND (e.expirationDate IS NULL OR e.expirationDate >= :currentDate)
        """)
    List<RuleExemption> findActiveExemptionsForRule(
        @Param("ruleId") String ruleId,
        @Param("currentDate") LocalDate currentDate
    );
}
