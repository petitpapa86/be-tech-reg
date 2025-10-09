package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.billing.BillingAccount;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.StripeCustomerId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.iam.domain.users.UserId;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

/**
 * JPA Entity for BillingAccount aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Entity
@Table(name = "billing_accounts")
public class BillingAccountEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "stripe_customer_id", nullable = false)
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
        entity.stripeCustomerId = billingAccount.getStripeCustomerId().value();
        entity.status = billingAccount.getStatus();
        
        if (billingAccount.getDefaultPaymentMethodId() != null) {
            entity.defaultPaymentMethodId = billingAccount.getDefaultPaymentMethodId().value();
        }
        
        if (billingAccount.getAccountBalance() != null) {
            entity.accountBalanceAmount = billingAccount.getAccountBalance().amount();
            entity.accountBalanceCurrency = billingAccount.getAccountBalance().currency().getCurrencyCode();
        }
        
        entity.createdAt = billingAccount.getCreatedAt();
        entity.updatedAt = billingAccount.getUpdatedAt();
        entity.version = billingAccount.getVersion();
        
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
        billingAccount.setStripeCustomerId(new StripeCustomerId(this.stripeCustomerId));
        billingAccount.setStatus(this.status);
        
        if (this.defaultPaymentMethodId != null) {
            billingAccount.setDefaultPaymentMethodId(new PaymentMethodId(this.defaultPaymentMethodId));
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

    // Getters and setters for JPA
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }

    public BillingAccountStatus getStatus() { return status; }
    public void setStatus(BillingAccountStatus status) { this.status = status; }

    public String getDefaultPaymentMethodId() { return defaultPaymentMethodId; }
    public void setDefaultPaymentMethodId(String defaultPaymentMethodId) { this.defaultPaymentMethodId = defaultPaymentMethodId; }

    public BigDecimal getAccountBalanceAmount() { return accountBalanceAmount; }
    public void setAccountBalanceAmount(BigDecimal accountBalanceAmount) { this.accountBalanceAmount = accountBalanceAmount; }

    public String getAccountBalanceCurrency() { return accountBalanceCurrency; }
    public void setAccountBalanceCurrency(String accountBalanceCurrency) { this.accountBalanceCurrency = accountBalanceCurrency; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
