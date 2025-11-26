package com.bcbs239.regtech.dataquality.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExemption;

import java.time.LocalDate;
import java.util.List;

/**
 * Domain repository interface for managing rule exemptions.
 * 
 * <p>This interface defines the contract for exemption persistence operations
 * without coupling to specific infrastructure implementations.</p>
 */
public interface RuleExemptionRepository {
    
    /**
     * Saves a rule exemption.
     * 
     * @param exemption The exemption to save
     * @return The saved exemption
     */
    RuleExemption save(RuleExemption exemption);
    
    /**
     * Finds all exemptions for a specific rule.
     * 
     * @param ruleId The rule ID
     * @return List of exemptions for the rule
     */
    List<RuleExemption> findByRuleId(String ruleId);
    
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
    List<RuleExemption> findActiveExemptions(
        String ruleId,
        String entityType,
        String entityId,
        LocalDate currentDate
    );
    
    /**
     * Finds all active exemptions for a specific rule.
     * 
     * @param ruleId The rule ID
     * @param currentDate The current date for checking validity
     * @return List of active exemptions
     */
    List<RuleExemption> findActiveExemptionsForRule(
        String ruleId,
        LocalDate currentDate
    );
}
