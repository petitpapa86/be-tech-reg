package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.*;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA Entity for Subscription aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "billing_account_id", nullable = false)
    private String billingAccountId;

    @Column(name = "stripe_subscription_id", nullable = false)
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false)
    private SubscriptionTier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Default constructor for JPA
    public SubscriptionEntity() {}

    /**
     * Convert domain aggregate to JPA entity
     */
    public static SubscriptionEntity fromDomain(Subscription subscription) {
        SubscriptionEntity entity = new SubscriptionEntity();
        
        if (subscription.getId() != null) {
            entity.id = subscription.getId().value();
        }
        entity.billingAccountId = subscription.getBillingAccountId().value();
        entity.stripeSubscriptionId = subscription.getStripeSubscriptionId().value();
        entity.tier = subscription.getTier();
        entity.status = subscription.getStatus();
        entity.startDate = subscription.getStartDate();
        entity.endDate = subscription.getEndDate();
        entity.createdAt = subscription.getCreatedAt();
        entity.updatedAt = subscription.getUpdatedAt();
        entity.version = subscription.getVersion();
        
        return entity;
    }

    /**
     * Convert JPA entity to domain aggregate
     */
    public Subscription toDomain() {
        // Create domain object using package-private constructor
        Subscription subscription = new Subscription();
        
        if (this.id != null) {
            subscription.setId(new SubscriptionId(this.id));
        }
        subscription.setBillingAccountId(new BillingAccountId(this.billingAccountId));
        subscription.setStripeSubscriptionId(new StripeSubscriptionId(this.stripeSubscriptionId));
        subscription.setTier(this.tier);
        subscription.setStatus(this.status);
        subscription.setStartDate(this.startDate);
        subscription.setEndDate(this.endDate);
        subscription.setCreatedAt(this.createdAt);
        subscription.setUpdatedAt(this.updatedAt);
        subscription.setVersion(this.version != null ? this.version : 0L);
        
        return subscription;
    }

    // Getters and setters for JPA
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBillingAccountId() { return billingAccountId; }
    public void setBillingAccountId(String billingAccountId) { this.billingAccountId = billingAccountId; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }

    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }

    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}