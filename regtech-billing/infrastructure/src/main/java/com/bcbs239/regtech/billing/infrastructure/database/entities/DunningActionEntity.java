package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.dunning.DunningAction;
import com.bcbs239.regtech.billing.domain.dunning.DunningActionId;
import com.bcbs239.regtech.billing.domain.dunning.DunningStep;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for DunningAction value object persistence.
 * Maps domain value object to database table structure.
 */
@Setter
@Getter
@Entity
@Table(name = "dunning_actions", schema = "billing")
public class DunningActionEntity {

    // Getters and setters for JPA
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
            DunningActionId.fromString(this.id).getValue().get(),
            this.step,
            this.actionType,
            this.notes,
            "SUCCESS".equals(this.result),
            this.executedAt
        );
    }

}

