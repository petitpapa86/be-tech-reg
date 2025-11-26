package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRule;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BusinessRule entity operations.
 */
@Repository
public interface BusinessRuleRepository extends JpaRepository<BusinessRule, String>, com.bcbs239.regtech.dataquality.rulesengine.repository.BusinessRuleRepository {
    
    /**
     * Finds all enabled rules.
     * 
     * @return List of all enabled rules
     */
    List<BusinessRule> findByEnabledTrue();
    
    /**
     * Finds a rule by its unique code.
     * 
     * @param ruleCode The rule code
     * @return Optional containing the rule if found
     */
    Optional<BusinessRule> findByRuleCode(String ruleCode);
    
    /**
     * Finds all rules of a specific type that are enabled.
     * 
     * @param ruleType The rule type
     * @return List of enabled rules ordered by execution order
     */
    List<BusinessRule> findByRuleTypeAndEnabledTrueOrderByExecutionOrder(RuleType ruleType);
    
    /**
     * Finds all rules in a specific category that are enabled.
     * 
     * @param category The rule category
     * @return List of enabled rules
     */
    List<BusinessRule> findByRuleCategoryAndEnabledTrue(String category);
    
    /**
     * Finds all rules for a specific regulation.
     * 
     * @param regulationId The regulation ID
     * @return List of rules for the regulation
     */
    List<BusinessRule> findByRegulationId(String regulationId);
    
    /**
     * Finds all active rules (enabled and within effective date range).
     * 
     * @param date The date to check
     * @return List of active rules ordered by execution order
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.enabled = true " +
           "AND r.effectiveDate <= :date " +
           "AND (r.expirationDate IS NULL OR r.expirationDate >= :date) " +
           "ORDER BY r.executionOrder")
    List<BusinessRule> findActiveRules(@Param("date") LocalDate date);
    
    /**
     * Finds all active rules of a specific type.
     * 
     * @param ruleType The rule type
     * @param date The date to check
     * @return List of active rules of the specified type
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.enabled = true " +
           "AND r.ruleType = :ruleType " +
           "AND r.effectiveDate <= :date " +
           "AND (r.expirationDate IS NULL OR r.expirationDate >= :date) " +
           "ORDER BY r.executionOrder")
    List<BusinessRule> findActiveRulesByType(
        @Param("ruleType") RuleType ruleType, 
        @Param("date") LocalDate date);
    
    /**
     * Finds all active rules for a specific category.
     * 
     * @param category The rule category
     * @param date The date to check
     * @return List of active rules in the category
     */
    @Query("SELECT r FROM BusinessRule r WHERE r.enabled = true " +
           "AND r.ruleCategory = :category " +
           "AND r.effectiveDate <= :date " +
           "AND (r.expirationDate IS NULL OR r.expirationDate >= :date) " +
           "ORDER BY r.executionOrder")
    List<BusinessRule> findActiveRulesByCategory(
        @Param("category") String category, 
        @Param("date") LocalDate date);
    
    /**
     * Counts the number of active rules.
     * 
     * @param date The date to check
     * @return Number of active rules
     */
    @Query("SELECT COUNT(r) FROM BusinessRule r WHERE r.enabled = true " +
           "AND r.effectiveDate <= :date " +
           "AND (r.expirationDate IS NULL OR r.expirationDate >= :date)")
    long countActiveRules(@Param("date") LocalDate date);
}
