package com.bcbs239.regtech.billing.domain.billing;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.iam.domain.users.UserId;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Currency;
import java.util.Objects;

/**
 * BillingAccount aggregate root - manages billing account lifecycle and status transitions.
 * Represents a customer's billing account with payment information and status tracking.
 */
@Setter
@Getter
public class BillingAccount {

    // Public setters for JPA/persistence layer
    // Getters
    private BillingAccountId id;
    private UserId userId;
    private StripeCustomerId stripeCustomerId;
    private BillingAccountStatus status;
    private PaymentMethodId defaultPaymentMethodId;
    private Money accountBalance;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    
    // Public constructor for JPA entity mapping
    public BillingAccount() {}
    
    /**
     * Factory method to create a new BillingAccount
     * 
     * @param userId The user this billing account belongs to
     * @param stripeCustomerId The Stripe customer ID for payment processing
     * @return New BillingAccount with PENDING_VERIFICATION status
     */
    public static BillingAccount create(UserId userId, StripeCustomerId stripeCustomerId) {
        Objects.requireNonNull(userId, "UserId cannot be null");
        Objects.requireNonNull(stripeCustomerId, "StripeCustomerId cannot be null");
        
        BillingAccount account = new BillingAccount();
        account.id = BillingAccountId.generate("BA");
        account.userId = userId;
        account.stripeCustomerId = stripeCustomerId;
        account.status = BillingAccountStatus.PENDING_VERIFICATION;
        account.accountBalance = Money.zero(Currency.getInstance("EUR"));
        account.createdAt = Instant.now();
        account.updatedAt = Instant.now();
        account.version = 0;
        
        return account;
    }
    
    /**
     * Activate the billing account with a default payment method.
     * Transitions from PENDING_VERIFICATION to ACTIVE status.
     * 
     * @param paymentMethodId The default payment method to set
     * @return Result indicating success or failure with error details
     */
    public Result<Void> activate(PaymentMethodId paymentMethodId) {
        Objects.requireNonNull(paymentMethodId, "PaymentMethodId cannot be null");
        
        if (this.status != BillingAccountStatus.PENDING_VERIFICATION) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                String.format("Cannot activate account from status: %s. Expected: %s", 
                    this.status, BillingAccountStatus.PENDING_VERIFICATION), "billing.account.invalid.status.transition"));
        }
        
        this.status = BillingAccountStatus.ACTIVE;
        this.defaultPaymentMethodId = paymentMethodId;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Suspend the billing account with a reason.
     * Can transition from ACTIVE or PAST_DUE to SUSPENDED.
     * 
     * @param reason The reason for suspension
     * @return Result indicating success or failure with error details
     */
    public Result<Void> suspend(String reason) {
        Objects.requireNonNull(reason, "Suspension reason cannot be null");
        
        if (this.status == BillingAccountStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of("ACCOUNT_CANCELLED", 
                "Cannot suspend cancelled account", "billing.account.cancelled"));
        }
        
        if (this.status == BillingAccountStatus.SUSPENDED) {
            return Result.failure(ErrorDetail.of("ALREADY_SUSPENDED", 
                "Account is already suspended", "billing.account.already.suspended"));
        }
        
        this.status = BillingAccountStatus.SUSPENDED;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Mark account as past due when payments fail.
     * Can transition from ACTIVE to PAST_DUE.
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> markAsPastDue() {
        if (this.status != BillingAccountStatus.ACTIVE) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                String.format("Cannot mark as past due from status: %s. Expected: %s", 
                    this.status, BillingAccountStatus.ACTIVE), "billing.account.invalid.status.transition"));
        }
        
        this.status = BillingAccountStatus.PAST_DUE;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Cancel the billing account permanently.
     * Can transition from any status except CANCELLED.
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> cancel() {
        if (this.status == BillingAccountStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of("ALREADY_CANCELLED", 
                "Account is already cancelled", "billing.account.already.cancelled"));
        }
        
        this.status = BillingAccountStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Reactivate account from PAST_DUE or SUSPENDED status.
     * 
     * @return Result indicating success or failure with error details
     */
    public Result<Void> reactivate() {
        if (this.status != BillingAccountStatus.PAST_DUE && 
            this.status != BillingAccountStatus.SUSPENDED) {
            return Result.failure(ErrorDetail.of("INVALID_STATUS_TRANSITION", 
                String.format("Cannot reactivate account from status: %s. Expected: %s or %s", 
                    this.status, BillingAccountStatus.PAST_DUE, BillingAccountStatus.SUSPENDED), "billing.account.invalid.status.transition"));
        }
        
        this.status = BillingAccountStatus.ACTIVE;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Check if the account can create new subscriptions.
     * Only ACTIVE accounts can create subscriptions.
     * 
     * @return true if account can create subscriptions, false otherwise
     */
    public boolean canCreateSubscription() {
        return this.status == BillingAccountStatus.ACTIVE;
    }
    
    /**
     * Update the default payment method for the account.
     * 
     * @param paymentMethodId The new default payment method
     * @return Result indicating success or failure with error details
     */
    public Result<Void> updateDefaultPaymentMethod(PaymentMethodId paymentMethodId) {
        Objects.requireNonNull(paymentMethodId, "PaymentMethodId cannot be null");
        
        if (this.status == BillingAccountStatus.CANCELLED) {
            return Result.failure(ErrorDetail.of("ACCOUNT_CANCELLED", 
                "Cannot update payment method for cancelled account", "billing.account.cancelled"));
        }
        
        this.defaultPaymentMethodId = paymentMethodId;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }
    
    /**
     * Update the account balance (for tracking purposes).
     * 
     * @param newBalance The new account balance
     * @return Result indicating success or failure with error details
     */
    public Result<Void> updateBalance(Money newBalance) {
        Objects.requireNonNull(newBalance, "Balance cannot be null");
        
        if (!newBalance.currency().equals(this.accountBalance.currency())) {
            return Result.failure(ErrorDetail.of("CURRENCY_MISMATCH", 
                String.format("Balance currency %s does not match account currency %s", 
                    newBalance.currency().getCurrencyCode(), 
                    this.accountBalance.currency().getCurrencyCode()), "billing.account.currency.mismatch"));
        }
        
        this.accountBalance = newBalance;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BillingAccount that = (BillingAccount) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("BillingAccount{id=%s, userId=%s, status=%s, version=%d}", 
            id, userId, status, version);
    }
}
