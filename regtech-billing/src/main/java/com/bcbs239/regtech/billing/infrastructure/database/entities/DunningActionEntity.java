package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.valueobjects.DunningAction;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningActionId;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningStep;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for DunningAction value object persistence.
 * Maps domain value object to database table structure.
 */
@Entity
@Table(name = "dunning_actions")
public class DunningActionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "dunning_case_id", nullable = false)
    private String dunningCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false)
    private DunningStep step;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "result", nullable = false, length = 50)
    private String result;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Default constructor for JPA
    public DunningActionEntity() {}

    /**
     * Convert domain value object to JPA entity
     */
    public static DunningActionEntity fromDomain(DunningAction action, String dunningCaseId) {
        DunningActionEntity entity = new DunningActionEntity();
        
        entity.id = UUID.randomUUID().toString(); // Generate ID for entity
        entity.dunningCaseId = dunningCaseId;
        entity.step = action.step();
        entity.actionType = action.actionType();
        entity.executedAt = action.executedAt();
        entity.result = action.successful() ? "SUCCESS" : "FAILURE";
        entity.notes = action.details();
        
        return entity;
    }

    /**
     * Convert JPA entity to domain value object
     */
    public DunningAction toDomain() {
        return new DunningAction(
            this.step,
            this.executedAt,
            this.actionType,
            this.notes,
            "SUCCESS".equals(this.result)
        );
    }

    // Getters and setters for JPA
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDunningCaseId() { return dunningCaseId; }
    public void setDunningCaseId(String dunningCaseId) { this.dunningCaseId = dunningCaseId; }

    public DunningStep getStep() { return step; }
    public void setStep(DunningStep step) { this.step = step; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}