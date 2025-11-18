package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

/**
 * Rule Violation entity representing detected violations of business rules.
 * 
 * <p>Violations are created when rule execution detects non-compliance.
 * They track resolution status and maintain an audit trail.</p>
 */
@Entity
@Table(name = "rule_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleViolation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;
    
    @Column(name = "rule_id", nullable = false, length = 100)
    private String ruleId;
    
    @Column(name = "execution_id", nullable = false)
    private Long executionId;
    
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
    @Convert(converter = JsonbConverter.class)
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
    
    /**
     * Marks the violation as resolved.
     * 
     * @param resolvedBy The user who resolved the violation
     * @param notes Resolution notes
     */
    public void resolve(String resolvedBy, String notes) {
        this.resolutionStatus = ResolutionStatus.RESOLVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }
    
    /**
     * Marks the violation as waived.
     * 
     * @param waivedBy The user who waived the violation
     * @param reason Waiver reason
     */
    public void waive(String waivedBy, String reason) {
        this.resolutionStatus = ResolutionStatus.WAIVED;
        this.resolvedAt = Instant.now();
        this.resolvedBy = waivedBy;
        this.resolutionNotes = reason;
    }
    
    /**
     * Checks if the violation is still open.
     * 
     * @return true if the violation is open or in progress
     */
    public boolean isOpen() {
        return resolutionStatus == ResolutionStatus.OPEN || 
               resolutionStatus == ResolutionStatus.IN_PROGRESS;
    }
}
