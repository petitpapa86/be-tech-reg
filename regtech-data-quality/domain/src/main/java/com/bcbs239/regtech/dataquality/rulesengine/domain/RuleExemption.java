package com.bcbs239.regtech.dataquality.rulesengine.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "rule_exemptions", schema = "dataquality")
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
    
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(effectiveDate) &&
               (expirationDate == null || !today.isAfter(expirationDate));
    }
    
    public boolean appliesTo(String type, String id) {
        if (!isActive()) return false;
        if (!entityType.equals(type)) return false;
        return entityId == null || entityId.equals(id);
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
