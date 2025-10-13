package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.billing.DunningCase;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningCaseId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningCaseStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningStep;
import com.bcbs239.regtech.billing.domain.valueobjects.DunningAction;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA Entity for DunningCase aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Entity
@Table(name = "dunning_cases", schema = "billing")
public class DunningCaseEntity {

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

    // Getters and setters for JPA
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBillingAccountId() { return billingAccountId; }
    public void setBillingAccountId(String billingAccountId) { this.billingAccountId = billingAccountId; }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public DunningCaseStatus getStatus() { return status; }
    public void setStatus(DunningCaseStatus status) { this.status = status; }

    public DunningStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(DunningStep currentStep) { this.currentStep = currentStep; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<DunningActionEntity> getActions() { return actions; }
    public void setActions(List<DunningActionEntity> actions) { this.actions = actions; }
}
