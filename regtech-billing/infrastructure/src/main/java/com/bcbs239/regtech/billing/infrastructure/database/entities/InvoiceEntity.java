package com.bcbs239.regtech.billing.infrastructure.database.entities;

import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.invoices.*;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;

/**
 * JPA Entity for Invoice aggregate persistence.
 * Maps domain aggregate to database table structure.
 */
@Getter
@Setter
@Entity
@Table(name = "invoices", schema = "billing")
public class InvoiceEntity {

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

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "stripe_invoice_id", nullable = false)
    private String stripeInvoiceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "subscription_amount_value", precision = 19, scale = 4)
    private BigDecimal subscriptionAmountValue;

    @Column(name = "subscription_amount_currency", length = 3, nullable = false)
    private String subscriptionAmountCurrency;

    @Column(name = "overage_amount_value", precision = 19, scale = 4)
    private BigDecimal overageAmountValue;

    @Column(name = "overage_amount_currency", length = 3, nullable = false)
    private String overageAmountCurrency;

    @Column(name = "total_amount_value", precision = 19, scale = 4)
    private BigDecimal totalAmountValue;

    @Column(name = "total_amount_currency", length = 3, nullable = false)
    private String totalAmountCurrency;

    @Column(name = "billing_period_start_date", nullable = false)
    private LocalDate billingPeriodStartDate;

    @Column(name = "billing_period_end_date", nullable = false)
    private LocalDate billingPeriodEndDate;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // Default constructor for JPA
    public InvoiceEntity() {}

    /**
     * Convert domain aggregate to JPA entity
     */
    public static InvoiceEntity fromDomain(Invoice invoice) {
        InvoiceEntity entity = new InvoiceEntity();
        
        if (invoice.getId() != null) {
            entity.id = invoice.getId().value();
        }
        if (invoice.getBillingAccountId().isPresent() && invoice.getBillingAccountId().getValue() != null) {
            entity.billingAccountId = invoice.getBillingAccountId().getValue().value();
        } else {
            entity.billingAccountId = null;
        }
        entity.invoiceNumber = invoice.getInvoiceNumber().value();
        entity.stripeInvoiceId = invoice.getStripeInvoiceId().value();
        entity.status = invoice.getStatus();
        
        if (invoice.getSubscriptionAmount() != null) {
            entity.subscriptionAmountValue = invoice.getSubscriptionAmount().amount();
            entity.subscriptionAmountCurrency = invoice.getSubscriptionAmount().currency().getCurrencyCode();
        }
        
        if (invoice.getOverageAmount() != null) {
            entity.overageAmountValue = invoice.getOverageAmount().amount();
            entity.overageAmountCurrency = invoice.getOverageAmount().currency().getCurrencyCode();
        }
        
        if (invoice.getTotalAmount() != null) {
            entity.totalAmountValue = invoice.getTotalAmount().amount();
            entity.totalAmountCurrency = invoice.getTotalAmount().currency().getCurrencyCode();
        }
        
        if (invoice.getBillingPeriod() != null) {
            entity.billingPeriodStartDate = invoice.getBillingPeriod().getStartDate();
            entity.billingPeriodEndDate = invoice.getBillingPeriod().getEndDate();
        }
        
        entity.issueDate = invoice.getIssueDate();
        entity.dueDate = invoice.getDueDate();
        entity.paidAt = invoice.getPaidAt();
        entity.sentAt = invoice.getSentAt();
        entity.createdAt = invoice.getCreatedAt();
        entity.updatedAt = invoice.getUpdatedAt();
        entity.version = invoice.getVersion();
        
        return entity;
    }

    /**
     * Convert JPA entity to domain aggregate
     */
    public Invoice toDomain() {
        // Create domain object using package-private constructor
        Invoice invoice = new Invoice();
        
        if (this.id != null) {
            invoice.setId(InvoiceId.fromString(this.id).getValue().get());
        }
        if (this.billingAccountId != null) {
            invoice.setBillingAccountId(Maybe.some(BillingAccountId.fromString(this.billingAccountId).getValue().get()));
        } else {
            invoice.setBillingAccountId(Maybe.none());
        }
        invoice.setInvoiceNumber(InvoiceNumber.fromString(this.invoiceNumber).getValue().get());
        invoice.setStripeInvoiceId(StripeInvoiceId.fromString(this.stripeInvoiceId).getValue().get());
        invoice.setStatus(this.status);
        
        if (this.subscriptionAmountValue != null && this.subscriptionAmountCurrency != null) {
            Currency subscriptionCurrency = Currency.getInstance(this.subscriptionAmountCurrency);
            invoice.setSubscriptionAmount(Money.of(this.subscriptionAmountValue, subscriptionCurrency));
        }
        
        if (this.overageAmountValue != null && this.overageAmountCurrency != null) {
            Currency overageCurrency = Currency.getInstance(this.overageAmountCurrency);
            invoice.setOverageAmount(Money.of(this.overageAmountValue, overageCurrency));
        }
        
        if (this.totalAmountValue != null && this.totalAmountCurrency != null) {
            Currency totalCurrency = Currency.getInstance(this.totalAmountCurrency);
            invoice.setTotalAmount(Money.of(this.totalAmountValue, totalCurrency));
        }
        
        if (this.billingPeriodStartDate != null && this.billingPeriodEndDate != null) {
            invoice.setBillingPeriod(BillingPeriod.of(YearMonth.from(this.billingPeriodStartDate)));
        }
        
        invoice.setIssueDate(this.issueDate);
        invoice.setDueDate(this.dueDate);
        invoice.setPaidAt(this.paidAt);
        invoice.setSentAt(this.sentAt);
        invoice.setCreatedAt(this.createdAt);
        invoice.setUpdatedAt(this.updatedAt);
        invoice.setVersion(this.version != null ? this.version : 0L);
        
        return invoice;
    }
}

