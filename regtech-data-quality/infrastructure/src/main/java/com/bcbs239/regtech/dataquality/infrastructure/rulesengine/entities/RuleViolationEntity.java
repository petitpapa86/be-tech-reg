package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities;

import com.bcbs239.regtech.dataquality.rulesengine.domain.ResolutionStatus;
import com.bcbs239.regtech.dataquality.rulesengine.domain.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "rule_violations", schema = "dataquality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleViolationEntity {
    @Id
    @SequenceGenerator(
        name = "rule_violations_violation_id_seq_gen",
        sequenceName = "dataquality.rule_violations_violation_id_seq",
        allocationSize = 20
    )
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_violations_violation_id_seq_gen")
    @Column(name = "violation_id")
    private Long violationId;
    
    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;
    
    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "batch_id", length = 100)
    private String batchId;
    
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;
    
    @Column(name = "violation_type", nullable = false, length = 100)
    private String violationType;
    
    @Column(name = "violation_description", nullable = false, columnDefinition = "TEXT")
    private String violationDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;
    
    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private Instant detectedAt = Instant.now();
    
    @Column(name = "violation_details", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> violationDetails;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", length = 20)
    @Builder.Default
    private ResolutionStatus resolutionStatus = ResolutionStatus.OPEN;
    
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    public void resolve(String resolvedBy, String notes) {
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }
    
    public void waive(String waivedBy, String reason) {
        this.resolutionStatus = ResolutionStatus.WAIVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = waivedBy;
        this.resolutionNotes = reason;
    }
    
    public boolean isOpen() {
        return resolutionStatus == ResolutionStatus.OPEN || resolutionStatus == ResolutionStatus.IN_PROGRESS;
    }
}
