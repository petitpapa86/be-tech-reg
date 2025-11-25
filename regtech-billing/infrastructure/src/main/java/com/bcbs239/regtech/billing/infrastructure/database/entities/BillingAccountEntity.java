package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccount;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.accounts.BillingAccountStatus;
import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.payments.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.domain.shared.valueobjects.UserId;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * JPA Entity for BillingAccount aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Setter
@Getter
@Entity
@Table(name = "billing_accounts", schema = "billing")
public class BillingAccountEntity {

    // Getters and setters for JPA
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;

    // Manual getters/setters for JPA compatibility
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BillingAccountStatus status;

    @Column(name = "default_payment_method_id")
    private String defaultPaymentMethodId;

    @Column(name = "account_balance_amount", precision = 19, scale = 4)
    private BigDecimal accountBalanceAmount;

    @Column(name = "account_balance_currency", length = 3)
    private String accountBalanceCurrency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Manual getters/setters for JPA compatibility
    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public BillingAccountStatus getStatus() {
        return status;
    }

    public void setStatus(BillingAccountStatus status) {
        this.status = status;
    }

    public String getDefaultPaymentMethodId() {
        return defaultPaymentMethodId;
    }

    public void setDefaultPaymentMethodId(String defaultPaymentMethodId) {
        this.defaultPaymentMethodId = defaultPaymentMethodId;
    }

    public BigDecimal getAccountBalanceAmount() {
        return accountBalanceAmount;
    }

    public void setAccountBalanceAmount(BigDecimal accountBalanceAmount) {
        this.accountBalanceAmount = accountBalanceAmount;
    }

    public String getAccountBalanceCurrency() {
        return accountBalanceCurrency;
    }

    public void setAccountBalanceCurrency(String accountBalanceCurrency) {
        this.accountBalanceCurrency = accountBalanceCurrency;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Default constructor for JPA
    public BillingAccountEntity() {}

    /**
     * Convert domain aggregate to JPA entity
     */
    public static BillingAccountEntity fromDomain(BillingAccount billingAccount) {
        BillingAccountEntity entity = new BillingAccountEntity();
        
        if (billingAccount.getId() != null) {
            entity.id = billingAccount.getId().value();
        }
        entity.userId = billingAccount.getUserId().getValue();
        entity.stripeCustomerId = billingAccount.getStripeCustomerId().isPresent() ? billingAccount.getStripeCustomerId().getValue().value() : null;
        entity.status = billingAccount.getStatus();
        
        entity.defaultPaymentMethodId = billingAccount.getDefaultPaymentMethodId().isPresent() ? billingAccount.getDefaultPaymentMethodId().getValue().value() : null;
        
        if (billingAccount.getAccountBalance() != null) {
            entity.accountBalanceAmount = billingAccount.getAccountBalance().amount();
            entity.accountBalanceCurrency = billingAccount.getAccountBalance().currency().getCurrencyCode();
        }
        
        entity.createdAt = billingAccount.getCreatedAt();
        entity.updatedAt = billingAccount.getUpdatedAt();
        // Do NOT copy version from domain into entity. Version is managed by JPA and copying a stale
        // version can cause OptimisticLockException on merge. Leave it null here and let the persistence
        // context manage it when performing updates through a managed entity.

        return entity;
    }

    /**
     * Convert JPA entity to domain aggregate
     */
    public BillingAccount toDomain() {
        // Create domain object using package-private constructor
        BillingAccount billingAccount = new BillingAccount();
        
        if (this.id != null) {
            billingAccount.setId(new BillingAccountId(this.id));
        }
        billingAccount.setUserId(UserId.fromString(this.userId));
        billingAccount.setStripeCustomerId(this.stripeCustomerId != null ? Maybe.some(new StripeCustomerId(this.stripeCustomerId)) : Maybe.none());
        billingAccount.setStatus(this.status);
        
        if (this.defaultPaymentMethodId != null) {
            billingAccount.setDefaultPaymentMethodId(Maybe.some(new PaymentMethodId(this.defaultPaymentMethodId)));
        } else {
            billingAccount.setDefaultPaymentMethodId(Maybe.none());
        }
        
        if (this.accountBalanceAmount != null && this.accountBalanceCurrency != null) {
            Money balance = Money.of(this.accountBalanceAmount, Currency.getInstance(this.accountBalanceCurrency));
            billingAccount.setAccountBalance(balance);
        }
        
        billingAccount.setCreatedAt(this.createdAt);
        billingAccount.setUpdatedAt(this.updatedAt);
        billingAccount.setVersion(this.version != null ? this.version : 0L);
        
        return billingAccount;
    }

}

