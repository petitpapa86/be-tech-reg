package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.subscriptions.*;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * JPA Entity for Subscription aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Setter
@Getter
@Entity
@Table(name = "subscriptions", schema = "billing")
public class SubscriptionEntity {

    // Getters and setters for JPA
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "billing_account_id", nullable = true)
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
        if (subscription.getBillingAccountId().isPresent() && subscription.getBillingAccountId().getValue() != null) {
            entity.billingAccountId = subscription.getBillingAccountId().getValue().value();
        } else {
            entity.billingAccountId = null;
        }
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
        if (this.billingAccountId != null) {
            subscription.setBillingAccountId(Maybe.some(new BillingAccountId(this.billingAccountId)));
        } else {
            subscription.setBillingAccountId(Maybe.none());
        }
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

}

