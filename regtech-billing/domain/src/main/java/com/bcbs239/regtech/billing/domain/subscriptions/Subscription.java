package com.bcbs239.regtech.billing.domain.subscriptions;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Maybe;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Subscription aggregate root - manages subscription lifecycle, tier management, and billing periods.
 * Represents a customer's subscription to a specific tier with start/end dates and status tracking.
 */
@Setter
@Getter
public class Subscription {

    // Public setters for JPA/persistence layer
    // Getters
    private SubscriptionId id;
    private Maybe<BillingAccountId> billingAccountId;
    private StripeSubscriptionId stripeSubscriptionId;
    private SubscriptionTier tier;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    
    // Public constructor for JPA entity mapping
    public Subscription() {
        this.billingAccountId = Maybe.none();
    }
    
    /**
     * Factory method to create a new Subscription
     * 
     * @param stripeSubscriptionId The Stripe subscription ID for payment processing
     * @param tier The subscription tier with pricing and limits
     * @return New Subscription with ACTIVE status
     */
    public static Subscription create(
                                    StripeSubscriptionId stripeSubscriptionId,
                                    SubscriptionTier tier) {
        Objects.requireNonNull(stripeSubscriptionId, "StripeSubscriptionId cannot be null");
        Objects.requireNonNull(tier, "SubscriptionTier cannot be null");
        
        Subscription subscription = new Subscription();
        subscription.id = null;
        subscription.stripeSubscriptionId = stripeSubscriptionId;
        subscription.tier = tier;
        subscription.status = SubscriptionStatus.ACTIVE;
        subscription.startDate = LocalDate.now();
        subscription.createdAt = Instant.now();
        subscription.updatedAt = Instant.now();
        subscription.version = 0;
        
        return subscription;
    }
    
    /**
     * Factory method to create a subscription with a specific start date
     * 
     * @param billingAccountId The billing account this subscription belongs to
     * @param stripeSubscriptionId The Stripe subscription ID for payment processing
     * @param tier The subscription tier with pricing and limits
     * @param startDate The start date for the subscription
     * @return New Subscription with ACTIVE status
     */
    public static Subscription create(Maybe<BillingAccountId> billingAccountId, 
                                    StripeSubscriptionId stripeSubscriptionId,
                                    SubscriptionTier tier,
                                    LocalDate startDate) {
        if (billingAccountId == null || billingAccountId.isEmpty()) {
            throw new IllegalArgumentException("BillingAccountId cannot be null or empty");
        }
        Objects.requireNonNull(startDate, "Start date cannot be null");
        
        Subscription subscription = create( stripeSubscriptionId, tier);
        subscription.startDate = startDate;
        subscription.billingAccountId = billingAccountId;
        
        return subscription;
    }
    
    /**
     * Cancel the subscription with a specific cancellation date.
     * Transitions to CANCELLED status and sets end date.
     * 
     * @param cancellationDate The date when the subscription should end
     * @return Result indicating success or failure with error details
     */
    public Result<Void> cancel(LocalDate cancellationDate) {
        Objects.requireNonNull(cancellationDate, "Cancellation date cannot be null");
        
        if (this.status == SubscriptionStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of("ALREADY_CANCELLED", 
                "Subscription is already cancelled", "subscription.already.cancelled"));
        }
        
        if (cancellationDate.isBefore(this.startDate)) {
            return Result.failure(ErrorDetail.of("INVALID_CANCELLATION_DATE", 
                "Cancellation date cannot be before start date", "subscription.invalid.cancellation.date"));
        }
        
        this.status = SubscriptionStatus.CANCELLED;
        this.endDate = cancellationDate;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Cancel the subscription immediately (end date = today).
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> cancel() {
        return cancel(LocalDate.now());
    }
    
    /**
     * Mark subscription as past due when payments fail.
     * Can transition from ACTIVE to PAST_DUE.
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> markAsPastDue() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                String.format("Cannot mark as past due from status: %s. Expected: %s", 
                    this.status, SubscriptionStatus.ACTIVE), "subscription.invalid.status.transition"));
        }
        
        this.status = SubscriptionStatus.PAST_DUE;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Pause the subscription temporarily.
     * Can transition from ACTIVE to PAUSED.
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> pause() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                String.format("Cannot pause subscription from status: %s. Expected: %s", 
                    this.status, SubscriptionStatus.ACTIVE), "subscription.invalid.status.transition"));
        }
        
        this.status = SubscriptionStatus.PAUSED;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Reactivate subscription from PAST_DUE or PAUSED status.
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> reactivate() {
        if (!this.status.canBeReactivated()) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                String.format("Cannot reactivate subscription from status: %s", this.status), "subscription.invalid.status.transition"));
        }
        
        this.status = SubscriptionStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Upgrade or downgrade the subscription tier.
     * Only allowed for ACTIVE subscriptions.
     * 
     * @param newTier The new subscription tier
     * @return Result indicating success or failure with error details
     */
    public Result<Void> changeTier(SubscriptionTier newTier) {
        Objects.requireNonNull(newTier, "New tier cannot be null");
        
        if (this.status != SubscriptionStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("SUBSCRIPTION_NOT_ACTIVE", 
                "Can only change tier for active subscriptions", "subscription.not.active"));
        }
        
        if (this.tier == newTier) {
            return Result.failure(ErrorDetail.of("SAME_TIER", 
                "New tier is the same as current tier", "subscription.same.tier"));
        }
        
        this.tier = newTier;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Update the Stripe subscription ID after successful Stripe subscription creation.
     * 
     * @param stripeSubscriptionId The Stripe subscription ID
     * @return Result indicating success or failure
     */
    public Result<Void> updateStripeSubscriptionId(StripeSubscriptionId stripeSubscriptionId) {
        Objects.requireNonNull(stripeSubscriptionId, "StripeSubscriptionId cannot be null");
        
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Get the monthly amount for this subscription based on its tier.
     * 
     * @return The monthly subscription amount
     */
    public Money getMonthlyAmount() {
        return tier.getMonthlyPrice();
    }
    
    /**
     * Check if the subscription is currently active.
     * 
     * @return true if subscription status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }
    
    /**
     * Check if the subscription is actively billing.
     * 
     * @return true if subscription is actively billing, false otherwise
     */
    public boolean isActiveBilling() {
        return status.isActiveBilling();
    }
    
    /**
     * Check if the subscription is terminated (cancelled).
     * 
     * @return true if subscription is cancelled, false otherwise
     */
    public boolean isTerminated() {
        return status.isTerminated();
    }
    
    /**
     * Check if the subscription requires payment attention.
     * 
     * @return true if subscription has payment issues, false otherwise
     */
    public boolean requiresPaymentAttention() {
        return status.requiresPaymentAttention();
    }
    
    /**
     * Check if the subscription is currently valid (not expired).
     * 
     * @return true if subscription is valid, false if expired
     */
    public boolean isValid() {
        if (endDate == null) {
            return true; // No end date means subscription is ongoing
        }
        return !LocalDate.now().isAfter(endDate);
    }
    
    /**
     * Check if the subscription is expired.
     * 
     * @return true if subscription has ended, false otherwise
     */
    public boolean isExpired() {
        return !isValid();
    }
    
    /**
     * Calculate overage charges based on actual usage.
     * 
     * @param actualUsage The actual usage count for the billing period
     * @return The overage charges as Money
     */
    public Money calculateOverageCharges(int actualUsage) {
        return tier.calculateOverageCharges(actualUsage);
    }
    
    /**
     * Check if usage exceeds the tier limit.
     * 
     * @param actualUsage The actual usage count
     * @return true if usage exceeds limit, false otherwise
     */
    public boolean isUsageOverLimit(int actualUsage) {
        return tier.isUsageOverLimit(actualUsage);
    }
    
    /**
     * Get the exposure limit for this subscription's tier.
     * 
     * @return The exposure limit
     */
    public int getExposureLimit() {
        return tier.getExposureLimit();
    }


}
