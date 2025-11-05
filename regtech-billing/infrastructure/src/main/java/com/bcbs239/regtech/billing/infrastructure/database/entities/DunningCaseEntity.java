package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.dunning.*;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA Entity for DunningCase aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Setter
@Getter
@Entity
@Table(name = "dunning_cases", schema = "billing")
public class DunningCaseEntity {

    // Getters and setters for JPA
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "billing_account_id", nullable = false)
    private String billingAccountId;

    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DunningCaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private DunningStep currentStep;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "dunningCaseId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DunningActionEntity> actions = new ArrayList<>();

    // Default constructor for JPA
    public DunningCaseEntity() {}

    /**
     * Convert domain aggregate to JPA entity
     */
    public static DunningCaseEntity fromDomain(DunningCase dunningCase) {
        DunningCaseEntity entity = new DunningCaseEntity();
        
        if (dunningCase.getId() != null) {
            entity.id = dunningCase.getId().value().toString();
        }
        entity.billingAccountId = dunningCase.getBillingAccountId().value();
        entity.invoiceId = dunningCase.getInvoiceId().value();
        entity.status = dunningCase.getStatus();
        entity.currentStep = dunningCase.getCurrentStep();
        entity.createdAt = dunningCase.getCreatedAt();
        entity.updatedAt = dunningCase.getUpdatedAt();
        entity.resolvedAt = dunningCase.getResolvedAt();
        entity.version = dunningCase.getVersion();
        
        // Convert actions
        if (dunningCase.getActions() != null) {
            entity.actions = dunningCase.getActions().stream()
                .map(action -> DunningActionEntity.fromDomain(action, entity.id))
                .collect(Collectors.toList());
        }
        
        return entity;
    }

    /**
     * Convert JPA entity to domain aggregate
     */
    public DunningCase toDomain() {
        // Create domain object using package-private constructor
        DunningCase dunningCase = new DunningCase();
        
        if (this.id != null) {
            dunningCase.setId(DunningCaseId.fromString(this.id).getValue().get());
        }
        dunningCase.setBillingAccountId(new BillingAccountId(this.billingAccountId));
        dunningCase.setInvoiceId(InvoiceId.fromString(this.invoiceId).getValue().get());
        dunningCase.setStatus(this.status);
        dunningCase.setCurrentStep(this.currentStep);
        dunningCase.setCreatedAt(this.createdAt);
        dunningCase.setUpdatedAt(this.updatedAt);
        dunningCase.setResolvedAt(this.resolvedAt);
        dunningCase.setVersion(this.version != null ? this.version : 0L);
        
        // Convert actions
        if (this.actions != null) {
            List<DunningAction> domainActions = this.actions.stream()
                .map(DunningActionEntity::toDomain)
                .collect(Collectors.toList());
            dunningCase.setActions(domainActions);
        }
        
        return dunningCase;
    }

}
