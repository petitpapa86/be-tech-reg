package com.bcbs239.regtech.dataquality.domain.rules;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for business rules.
 * This interface defines the contract without coupling to infrastructure.
 */
public interface IBusinessRuleRepository {
    
    /**
     * Finds all enabled rules.
     */
    List<BusinessRuleDto> findByEnabledTrue();
    
    /**
     * Finds a rule by its unique code.
     */
    Optional<BusinessRuleDto> findByRuleCode(String ruleCode);
    
    /**
     * Finds all rules of a specific type that are enabled.
     */
    List<BusinessRuleDto> findByRuleTypeAndEnabledTrueOrderByExecutionOrder(RuleType ruleType);
    
    /**
     * Finds all rules in a specific category that are enabled.
     */
    List<BusinessRuleDto> findByRuleCategoryAndEnabledTrue(String category);
    
    /**
     * Finds all active rules (enabled and within effective date range).
     */
    List<BusinessRuleDto> findActiveRules(LocalDate date);
}
