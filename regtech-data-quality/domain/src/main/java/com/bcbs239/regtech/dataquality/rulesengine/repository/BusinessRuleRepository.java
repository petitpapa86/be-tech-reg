package com.bcbs239.regtech.dataquality.rulesengine.repository;

import com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRule;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleType;

import java.util.List;
import java.util.Optional;

public interface BusinessRuleRepository {
    List<BusinessRule> findByEnabledTrue();
    List<BusinessRule> findByRuleTypeAndEnabledTrueOrderByExecutionOrder(RuleType ruleType);
    List<BusinessRule> findByRuleCategoryAndEnabledTrue(String category);
    Optional<BusinessRule> findByRuleCode(String ruleCode);
}
