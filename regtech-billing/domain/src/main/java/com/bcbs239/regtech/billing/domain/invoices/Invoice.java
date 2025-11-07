package com.bcbs239.regtech.billing.domain.invoices;


import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Invoice aggregate root representing billing invoices with line items and payment tracking.
 * Handles invoice lifecycle from creation to payment with automatic line item generation.
 */
@Setter
@Getter
public class Invoice {

    // Public setters for JPA/persistence layer
    private InvoiceId id;
    private Maybe<BillingAccountId> billingAccountId;
    private InvoiceNumber invoiceNumber;
    private StripeInvoiceId stripeInvoiceId;
    private InvoiceStatus status;
    private Money subscriptionAmount;
    private Money overageAmount;
    private Money totalAmount;
    private BillingPeriod billingPeriod;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private Instant paidAt;
    private Instant sentAt;
    private List<InvoiceLineItem> lineItems;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Public constructor for JPA entity mapping
    public Invoice() {
        this.lineItems = new ArrayList<>();
        billingAccountId = Maybe.none();
    }

    // Private constructor for factory methods
    private Invoice(Supplier<Instant> clock, Supplier<LocalDate> dateSupplier) {
        this.lineItems = new ArrayList<>();
        this.createdAt = clock.get();
        this.updatedAt = clock.get();
        this.version = 0;
        this.issueDate = dateSupplier.get();
        this.dueDate = dateSupplier.get().plusDays(14); // 14 days payment terms
    }

    /**
     * Factory method to create a new invoice with automatic line item generation
     */
    public static Result<Invoice> create(Maybe<BillingAccountId> billingAccountId,
                                         StripeInvoiceId stripeInvoiceId,
                                         Money subscriptionAmount,
                                         Money overageAmount,
                                         BillingPeriod billingPeriod,
                                         Supplier<Instant> clock,
                                         Supplier<LocalDate> dateSupplier) {
        
        // Validate inputs
        if (billingAccountId == null || billingAccountId.isEmpty()) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "BillingAccountId cannot be null or empty", null);
        }
        if (stripeInvoiceId == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "StripeInvoiceId cannot be null", null);
        }
        if (subscriptionAmount == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "Subscription amount cannot be null", null);
        }
        if (overageAmount == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "Overage amount cannot be null", null);
        }
        if (billingPeriod == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "Billing period cannot be null", null);
        }
        if (clock == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "Clock supplier cannot be null", null);
        }
        if (dateSupplier == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.VALIDATION_ERROR, "Date supplier cannot be null", null);
        }

        // Validate currency consistency
        if (!subscriptionAmount.currency().equals(overageAmount.currency())) {
            return Result.failure("CURRENCY_MISMATCH", ErrorType.BUSINESS_RULE_ERROR, "Subscription and overage amounts must have the same currency", null);
        }

        Invoice invoice = new Invoice(clock, dateSupplier);
        invoice.id = null;
        invoice.billingAccountId = billingAccountId;
        invoice.invoiceNumber = InvoiceNumber.generate();
        invoice.stripeInvoiceId = stripeInvoiceId;
        invoice.status = InvoiceStatus.PENDING;
        invoice.subscriptionAmount = subscriptionAmount;
        invoice.overageAmount = overageAmount;
        invoice.billingPeriod = billingPeriod;

        // Calculate total amount
        Result<Money> totalResult = subscriptionAmount.add(overageAmount);
        if (totalResult.isFailure()) {
            return Result.failure(totalResult.getError().get());
        }
        invoice.totalAmount = totalResult.getValue().get();

        // Generate line items automatically
        Result<Void> lineItemResult = invoice.generateLineItems();
        if (lineItemResult.isFailure()) {
            return Result.failure(lineItemResult.getError().get());
        }

        return Result.success(invoice);
    }

    /**
     * Factory method to create a pro-rated invoice for partial billing periods
     */
    public static Result<Invoice> createProRated(Maybe<BillingAccountId> billingAccountId,
                                               StripeInvoiceId stripeInvoiceId,
                                               Money monthlySubscriptionAmount,
                                               Money overageAmount,
                                               BillingPeriod billingPeriod,
                                               LocalDate serviceStartDate,
                                               Supplier<Instant> clock,
                                               Supplier<LocalDate> dateSupplier) {
        
        if (monthlySubscriptionAmount == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.BUSINESS_RULE_ERROR, "Monthly subscription amount cannot be null", null);
        }
        if (serviceStartDate == null) {
            return Result.failure("INVALID_INVOICE", ErrorType.BUSINESS_RULE_ERROR, "Service start date cannot be null", null);
        }

        // Calculate pro-rated subscription amount
        Result<Money> proRatedResult = billingPeriod.calculateProRatedAmount(monthlySubscriptionAmount, serviceStartDate);
        if (proRatedResult.isFailure()) {
            return Result.failure(proRatedResult.getError().get());
        }

        return create(billingAccountId, stripeInvoiceId, proRatedResult.getValue().get(), overageAmount, billingPeriod, clock, dateSupplier);
    }

    /**
     * Mark invoice as paid with timestamp
     */
    public Result<Void> markAsPaid(Instant paidAt, Supplier<Instant> clock) {
        if (paidAt == null) {
            return Result.failure("INVALID_PAYMENT", ErrorType.BUSINESS_RULE_ERROR, "Paid timestamp cannot be null", null);
        }
        if (clock == null) {
            return Result.failure("INVALID_PAYMENT", ErrorType.BUSINESS_RULE_ERROR, "Clock supplier cannot be null", null);
        }
        
        if (this.status == InvoiceStatus.PAID) {
            return Result.failure("ALREADY_PAID", ErrorType.BUSINESS_RULE_ERROR, "Invoice is already marked as paid", null);
        }
        
        if (this.status == InvoiceStatus.VOIDED) {
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR, "Cannot mark voided invoice as paid", null);
        }
        
        this.status = InvoiceStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = clock.get();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Mark invoice as paid (simplified version without parameters)
     */
    public Result<Invoice> markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(this);
    }

    /**
     * Mark invoice as payment failed
     */
    public Result<Invoice> markAsPaymentFailed() {
        this.status = InvoiceStatus.FAILED;
        this.updatedAt = Instant.now();
        this.version++;
        
        return Result.success(this);
    }

    /**
     * Mark invoice as overdue
     */
    public Result<Void> markAsOverdue(Supplier<Instant> clock) {
        if (clock == null) {
            return Result.failure("INVALID_OPERATION", ErrorType.BUSINESS_RULE_ERROR, "Clock supplier cannot be null", null);
        }
        
        if (this.status != InvoiceStatus.PENDING) {
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR, "Cannot mark as overdue from status: " + this.status, null);
        }
        
        this.status = InvoiceStatus.OVERDUE;
        this.updatedAt = clock.get();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Mark invoice as failed
     */
    public Result<Void> markAsFailed(Supplier<Instant> clock) {
        if (clock == null) {
            return Result.failure("INVALID_OPERATION", ErrorType.BUSINESS_RULE_ERROR, "Clock supplier cannot be null", null);
        }
        
        if (this.status == InvoiceStatus.PAID || this.status == InvoiceStatus.VOIDED) {
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR, "Cannot mark paid or voided invoice as failed", null);
        }
        
        this.status = InvoiceStatus.FAILED;
        this.updatedAt = clock.get();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Void the invoice (cancel it)
     */
    public Result<Void> voidInvoice(Supplier<Instant> clock) {
        if (clock == null) {
            return Result.failure("INVALID_OPERATION", ErrorType.BUSINESS_RULE_ERROR, "Clock supplier cannot be null", null);
        }
        
        if (this.status == InvoiceStatus.PAID) {
            return Result.failure("INVALID_STATUS_TRANSITION", ErrorType.BUSINESS_RULE_ERROR, "Cannot void a paid invoice", null);
        }
        
        this.status = InvoiceStatus.VOIDED;
        this.updatedAt = clock.get();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Mark invoice as sent
     */
    public Result<Void> markAsSent(Instant sentAt, Supplier<Instant> clock) {
        if (sentAt == null) {
            return Result.failure("INVALID_SENT_DATE", ErrorType.BUSINESS_RULE_ERROR, "Sent timestamp cannot be null", null);
        }
        if (clock == null) {
            return Result.failure("INVALID_OPERATION", ErrorType.BUSINESS_RULE_ERROR, "Clock supplier cannot be null", null);
        }
        
        if (this.status == InvoiceStatus.DRAFT) {
            this.status = InvoiceStatus.PENDING;
        }
        
        this.sentAt = sentAt;
        this.updatedAt = clock.get();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Check if invoice is overdue based on due date
     */
    public boolean isOverdue(Supplier<LocalDate> dateSupplier) {
        if (dateSupplier == null) {
            return false; // Safe default when no date supplier provided
        }
        return dateSupplier.get().isAfter(dueDate) && 
               (status == InvoiceStatus.PENDING || status == InvoiceStatus.OVERDUE);
    }

    /**
     * Check if invoice can be paid
     */
    public boolean canBePaid() {
        return status == InvoiceStatus.PENDING || status == InvoiceStatus.OVERDUE;
    }

    /**
     * Check if invoice has overage charges
     */
    public boolean hasOverageCharges() {
        return overageAmount != null && overageAmount.isPositive();
    }

    /**
     * Get days until due date (negative if overdue)
     */
    public long getDaysUntilDue(Supplier<LocalDate> dateSupplier) {
        if (dateSupplier == null) {
            return 0; // Safe default when no date supplier provided
        }
        return dateSupplier.get().until(dueDate).getDays();
    }

    /**
     * Generate line items automatically based on subscription and overage amounts
     */
    private Result<Void> generateLineItems() {
        this.lineItems.clear();

        // Add subscription line item
        if (subscriptionAmount.isPositive()) {
            Result<InvoiceLineItem> subscriptionLineResult = InvoiceLineItem.forSubscription(
                "STARTER", subscriptionAmount, billingPeriod);
            if (subscriptionLineResult.isFailure()) {
                return Result.failure(subscriptionLineResult.getError().get());
            }
            this.lineItems.add(subscriptionLineResult.getValue().get());
        }

        // Add overage line item if applicable
        if (overageAmount.isPositive()) {
            // Calculate overage count (assuming €0.05 per exposure)
            Money overageRate = Money.of("0.05", overageAmount.currency());
            int overageCount = overageAmount.amount().divide(overageRate.amount()).intValue();
            
            Result<InvoiceLineItem> overageLineResult = InvoiceLineItem.forOverage(overageCount, overageRate);
            if (overageLineResult.isFailure()) {
                return Result.failure(overageLineResult.getError().get());
            }
            this.lineItems.add(overageLineResult.getValue().get());
        }

        return Result.success(null);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Invoice{id=%s, number=%s, status=%s, total=%s}", 
            id, invoiceNumber, status, totalAmount);
    }
}

