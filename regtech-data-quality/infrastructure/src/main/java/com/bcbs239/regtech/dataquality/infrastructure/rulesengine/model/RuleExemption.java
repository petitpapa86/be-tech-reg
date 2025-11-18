package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Rule Exemption entity representing exceptions from specific rules.
 * 
 * <p>Exemptions allow certain entities to bypass rule validation under
 * controlled conditions with proper approval and audit trail.</p>
 */
@Entity
@Table(name = "rule_exemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleExemption {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exemption_id")
    private Long exemptionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BusinessRule rule;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "entity_id", length = 100)
    private String entityId;
    
    @Column(name = "exemption_reason", nullable = false, columnDefinition = "TEXT")
    private String exemptionReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "exemption_type", nullable = false, length = 50)
    private ExemptionType exemptionType;
    
    @Column(name = "approved_by", nullable = false, length = 100)
    private String approvedBy;
    
    @Column(name = "approval_date", nullable = false)
    private LocalDate approvalDate;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "expiration_date")
    private LocalDate expirationDate;
    
    @Column(columnDefinition = "JSONB")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> conditions;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    /**
     * Checks if the exemption is currently active.
     * 
     * @return true if the exemption is active today
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(effectiveDate) &&
               (expirationDate == null || !today.isAfter(expirationDate));
    }
    
    /**
     * Checks if this exemption applies to a specific entity.
     * 
     * @param type The entity type
     * @param id The entity ID
     * @return true if the exemption applies
     */
    public boolean appliesTo(String type, String id) {
        if (!isActive()) {
            return false;
        }
        
        if (!entityType.equals(type)) {
            return false;
        }
        
        // If entityId is null, exemption applies to all entities of this type
        return entityId == null || entityId.equals(id);
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
