package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Business Rule entity representing configurable validation and business logic rules.
 * 
 * <p>This entity encapsulates dynamic rules that can be configured at runtime
 * without code deployment. Rules can have parameters, exemptions, and execution history.</p>
 */
@Entity
@Table(name = "business_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessRule {
    
    @Id
    @Column(name = "rule_id", length = 100)
    private String ruleId;
    
    @Column(name = "regulation_id", nullable = false, length = 100)
    private String regulationId;
    
    @Column(name = "template_id", length = 100)
    private String templateId;
    
    @Column(name = "rule_name", nullable = false)
    private String ruleName;
    
    @Column(name = "rule_code", unique = true, length = 50)
    private String ruleCode;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;
    
    @Column(name = "rule_category", length = 50)
    private String ruleCategory;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;
    
    @Column(name = "business_logic", nullable = false, columnDefinition = "TEXT")
    private String businessLogic;
    
    @Column(name = "execution_order", nullable = false)
    @Builder.Default
    private Integer executionOrder = 100;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;
    
    @Version
    private Integer version;
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RuleParameter> parameters = new ArrayList<>();
    
    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RuleExemption> exemptions = new ArrayList<>();
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    /**
     * Checks if the rule is applicable on a specific date.
     * A rule is applicable if the date falls within its effective period.
     * 
     * @param date The date to check
     * @return true if the rule is applicable on the given date
     */
    public boolean isApplicableOn(LocalDate date) {
        return !date.isBefore(effectiveDate) && 
               (expirationDate == null || !date.isAfter(expirationDate));
    }
    
    /**
     * Checks if the rule is currently active.
     * A rule is active if it's enabled and applicable today.
     * 
     * @return true if the rule is active
     */
    public boolean isActive() {
        return enabled && isApplicableOn(LocalDate.now());
    }
    
    /**
     * Adds a parameter to this rule.
     * 
     * @param parameter The parameter to add
     */
    public void addParameter(RuleParameter parameter) {
        parameters.add(parameter);
        parameter.setRule(this);
    }
    
    /**
     * Removes a parameter from this rule.
     * 
     * @param parameter The parameter to remove
     */
    public void removeParameter(RuleParameter parameter) {
        parameters.remove(parameter);
        parameter.setRule(null);
    }
    
    /**
     * Adds an exemption to this rule.
     * 
     * @param exemption The exemption to add
     */
    public void addExemption(RuleExemption exemption) {
        exemptions.add(exemption);
        exemption.setRule(this);
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
