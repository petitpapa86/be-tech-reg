package com.bcbs239.regtech.billing.domain.accounts;

import com.bcbs239.regtech.billing.domain.invoices.Invoice;
import com.bcbs239.regtech.billing.domain.invoices.StripeInvoiceId;
import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.subscriptions.StripeSubscriptionId;
import com.bcbs239.regtech.billing.domain.subscriptions.Subscription;
import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;

import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.iam.domain.users.UserId;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
    private Maybe<StripeCustomerId> stripeCustomerId;
    private BillingAccountStatus status;
    private Maybe<PaymentMethodId> defaultPaymentMethodId;
    private Money accountBalance;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;
    
    // Subscriptions associated with this billing account
    private final Set<Subscription> subscriptions = new HashSet<>();
    
    // Invoices associated with this billing account
    private final Set<Invoice> invoices = new HashSet<>();
    
    // Public constructor for JPA entity mapping
    public BillingAccount() {}
    
    /**
     * Builder for creating BillingAccount instances.
     * Handles complex initialization with optional fields.
     */
    public static class Builder {
        private UserId userId;
        private Maybe<StripeCustomerId> stripeCustomerId = Maybe.none();
        private Instant createdAt;
        private Instant updatedAt;
        private boolean createDefaults = false; // Flag to create default subscription and invoice

        public Builder userId(UserId userId) {
            this.userId = userId;
            return this;
        }

        public Builder stripeCustomerId(StripeCustomerId stripeCustomerId) {
            this.stripeCustomerId = Maybe.some(stripeCustomerId);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder withDefaults() {
            this.createDefaults = true;
            return this;
        }

        public BillingAccount build() {
            Objects.requireNonNull(userId, "UserId cannot be null");
            Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
            Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");

            BillingAccount account = new BillingAccount();
            account.id = null;
            account.userId = userId;
            account.stripeCustomerId = stripeCustomerId;
            account.status = BillingAccountStatus.PENDING_VERIFICATION;
            account.defaultPaymentMethodId = Maybe.none();
            account.accountBalance = Money.zero(Currency.getInstance("EUR"));
            account.createdAt = createdAt;
            account.updatedAt = updatedAt;
            account.version = 0;

            if (createDefaults) {
                // Create default subscription
                Result<StripeSubscriptionId> defaultStripeIdResult = StripeSubscriptionId.fromString("default");
                if (!defaultStripeIdResult.isSuccess()) {
                    throw new IllegalStateException("Failed to create default StripeSubscriptionId: " + defaultStripeIdResult.getError());
                }
                StripeSubscriptionId defaultStripeId = defaultStripeIdResult.getValue().orElseThrow(
                    () -> new IllegalStateException("Failed to obtain StripeSubscriptionId value despite success: " + defaultStripeIdResult.getError())
                );
                Subscription defaultSubscription = Subscription.create( defaultStripeId, SubscriptionTier.STARTER);
                account.subscriptions.add(defaultSubscription);

                // Create default invoice
                Result<StripeInvoiceId> defaultInvoiceIdResult = StripeInvoiceId.fromString("default");
                if (!defaultInvoiceIdResult.isSuccess()) {
                    throw new IllegalStateException("Failed to create default StripeInvoiceId: " + defaultInvoiceIdResult.getError());
                }
                StripeInvoiceId defaultInvoiceId = defaultInvoiceIdResult.getValue().orElseThrow(
                    () -> new IllegalStateException("Failed to obtain StripeInvoiceId value despite success: " + defaultInvoiceIdResult.getError())
                );
                Money subscriptionAmount = defaultSubscription.getMonthlyAmount();
                Money overageAmount = Money.zero(Currency.getInstance("EUR"));
                BillingPeriod currentPeriod = BillingPeriod.current();

                Result<Invoice> invoiceResult = Invoice.create(
                    Maybe.some(account.id),
                    defaultInvoiceId,
                    subscriptionAmount,
                    overageAmount,
                    currentPeriod,
                    () -> createdAt,
                    LocalDate::now
                );

                if (invoiceResult.isSuccess()) {
                    account.invoices.add(invoiceResult.getValue().orElseThrow(
                        () -> new IllegalStateException("Invoice creation reported success but value missing: " + invoiceResult.getError())
                    ));
                }
            }

            return account;
        }
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
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                String.format("Cannot activate account from status: %s. Expected: %s", 
                    this.status, BillingAccountStatus.PENDING_VERIFICATION), "billing.account.invalid.status.transition");
        }
        
        this.status = BillingAccountStatus.ACTIVE;
        this.defaultPaymentMethodId = Maybe.some(paymentMethodId);
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
            return Result.failure("ACCOUNT_CANCELLED", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot suspend cancelled account", "billing.account.cancelled");
        }
        
        if (this.status == BillingAccountStatus.SUSPENDED) {
            return Result.failure("ALREADY_SUSPENDED", ErrorType.BUSINESS_RULE_ERROR,
                "Account is already suspended", "billing.account.already.suspended");
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
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                String.format("Cannot mark as past due from status: %s. Expected: %s", 
                    this.status, BillingAccountStatus.ACTIVE), "billing.account.invalid.status.transition");
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
            return Result.failure("ALREADY_CANCELLED", ErrorType.BUSINESS_RULE_ERROR,
                "Account is already cancelled", "billing.account.already.cancelled");
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
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR,
                String.format("Cannot reactivate account from status: %s. Expected: %s or %s", 
                    this.status, BillingAccountStatus.PAST_DUE, BillingAccountStatus.SUSPENDED), "billing.account.invalid.status.transition");
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
     * Create a new subscription for this billing account.
     * Only ACTIVE accounts can create subscriptions.
     * 
     * @param stripeSubscriptionId The Stripe subscription ID
     * @param tier The subscription tier
     * @return Result containing the created Subscription or error details
     */
    public Result<Subscription> createSubscription(StripeSubscriptionId stripeSubscriptionId, SubscriptionTier tier) {
        Objects.requireNonNull(stripeSubscriptionId, "StripeSubscriptionId cannot be null");
        Objects.requireNonNull(tier, "SubscriptionTier cannot be null");
        
        if (!canCreateSubscription()) {
            return Result.failure("ACCOUNT_NOT_ACTIVE", ErrorType.BUSINESS_RULE_ERROR,
                "Cannot create subscription for non-active account", "billing.account.not.active");
        }
        
        Subscription subscription = Subscription.create(
            stripeSubscriptionId,
            tier
        );
        
        return Result.success(subscription);
    }
    
    /**
     * Update the Stripe customer ID for the account.
     * Can only be set once - subsequent calls will fail.
     * 
     * @param stripeCustomerId The Stripe customer ID to set
     * @return Result indicating success or failure with error details
     */
    public Result<Void> updateStripeCustomerId(StripeCustomerId stripeCustomerId) {
        Objects.requireNonNull(stripeCustomerId, "StripeCustomerId cannot be null");
        
        if (this.stripeCustomerId.isPresent()) {
            return Result.failure("CUSTOMER_ID_ALREADY_SET", ErrorType.BUSINESS_RULE_ERROR,
                "Stripe customer ID is already set for this account", "billing.account.customer.id.already.set");
        }
        
        this.stripeCustomerId = Maybe.some(stripeCustomerId);
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
            return Result.failure("CURRENCY_MISMATCH", ErrorType.BUSINESS_RULE_ERROR,
                String.format("Balance currency %s does not match account currency %s", 
                    newBalance.currency().getCurrencyCode(), 
                    this.accountBalance.currency().getCurrencyCode()), "billing.account.currency.mismatch");
        }
        
        this.accountBalance = newBalance;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Configure Stripe customer information after successful customer creation.
     * Sets the default payment method and Stripe customer ID.
     * 
     * @param paymentMethodId The default payment method ID
     * @param stripeCustomerId The Stripe customer ID
     * @return Result indicating success or failure
     */
    public Result<Void> configureStripeCustomer(PaymentMethodId paymentMethodId, StripeCustomerId stripeCustomerId) {
        Objects.requireNonNull(paymentMethodId, "PaymentMethodId cannot be null");
        Objects.requireNonNull(stripeCustomerId, "StripeCustomerId cannot be null");
        
        this.defaultPaymentMethodId = Maybe.some(paymentMethodId);
        this.stripeCustomerId = Maybe.some(stripeCustomerId);
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Finalize the billing account after successful payment verification.
     * Transitions from PENDING_VERIFICATION to ACTIVE status.
     */
    public void finalizeAccount() {
        if (this.status == BillingAccountStatus.PENDING_VERIFICATION) {
            this.status = BillingAccountStatus.ACTIVE;
            this.updatedAt = Instant.now();
            this.version++;
        }
    }
}


