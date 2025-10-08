package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.billing.domain.valueobjects.*;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Invoice aggregate root representing billing invoices with line items and payment tracking.
 * Handles invoice lifecycle from creation to payment with automatic line item generation.
 */
public class Invoice {
    
    private InvoiceId id;
    private BillingAccountId billingAccountId;
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

    // Package-private constructor for JPA entity mapping
    Invoice() {
        this.lineItems = new ArrayList<>();
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
    public static Result<Invoice> create(BillingAccountId billingAccountId,
                                       StripeInvoiceId stripeInvoiceId,
                                       Money subscriptionAmount,
                                       Money overageAmount,
                                       BillingPeriod billingPeriod,
                                       Supplier<Instant> clock,
                                       Supplier<LocalDate> dateSupplier) {
        
        // Validate inputs
        if (billingAccountId == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "BillingAccountId cannot be null"));
        }
        if (stripeInvoiceId == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "StripeInvoiceId cannot be null"));
        }
        if (subscriptionAmount == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Subscription amount cannot be null"));
        }
        if (overageAmount == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Overage amount cannot be null"));
        }
        if (billingPeriod == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Billing period cannot be null"));
        }
        if (clock == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Clock supplier cannot be null"));
        }
        if (dateSupplier == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Date supplier cannot be null"));
        }

        // Validate currency consistency
        if (!subscriptionAmount.currency().equals(overageAmount.currency())) {
            return Result.failure(new ErrorDetail("CURRENCY_MISMATCH", 
                "Subscription and overage amounts must have the same currency"));
        }

        Invoice invoice = new Invoice(clock, dateSupplier);
        invoice.id = InvoiceId.generate();
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
    public static Result<Invoice> createProRated(BillingAccountId billingAccountId,
                                               StripeInvoiceId stripeInvoiceId,
                                               Money monthlySubscriptionAmount,
                                               Money overageAmount,
                                               BillingPeriod billingPeriod,
                                               LocalDate serviceStartDate,
                                               Supplier<Instant> clock,
                                               Supplier<LocalDate> dateSupplier) {
        
        if (monthlySubscriptionAmount == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Monthly subscription amount cannot be null"));
        }
        if (serviceStartDate == null) {
            return Result.failure(new ErrorDetail("INVALID_INVOICE", "Service start date cannot be null"));
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
            return Result.failure(new ErrorDetail("INVALID_PAYMENT", "Paid timestamp cannot be null"));
        }
        if (clock == null) {
            return Result.failure(new ErrorDetail("INVALID_PAYMENT", "Clock supplier cannot be null"));
        }
        
        if (this.status == InvoiceStatus.PAID) {
            return Result.failure(new ErrorDetail("ALREADY_PAID", "Invoice is already marked as paid"));
        }
        
        if (this.status == InvoiceStatus.VOIDED) {
            return Result.failure(new ErrorDetail("INVALID_STATUS_TRANSITION", "Cannot mark voided invoice as paid"));
        }
        
        this.status = InvoiceStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = clock.get();
        this.version++;
        
        return Result.success(null);
    }

    /**
     * Mark invoice as overdue
     */
    public Result<Void> markAsOverdue(Supplier<Instant> clock) {
        if (clock == null) {
            return Result.failure(new ErrorDetail("INVALID_OPERATION", "Clock supplier cannot be null"));
        }
        
        if (this.status != InvoiceStatus.PENDING) {
            return Result.failure(new ErrorDetail("INVALID_STATUS_TRANSITION", 
                "Cannot mark as overdue from status: " + this.status));
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
            return Result.failure(new ErrorDetail("INVALID_OPERATION", "Clock supplier cannot be null"));
        }
        
        if (this.status == InvoiceStatus.PAID || this.status == InvoiceStatus.VOIDED) {
            return Result.failure(new ErrorDetail("INVALID_STATUS_TRANSITION", 
                "Cannot mark paid or voided invoice as failed"));
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
            return Result.failure(new ErrorDetail("INVALID_OPERATION", "Clock supplier cannot be null"));
        }
        
        if (this.status == InvoiceStatus.PAID) {
            return Result.failure(new ErrorDetail("INVALID_STATUS_TRANSITION", 
                "Cannot void a paid invoice"));
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
            return Result.failure(new ErrorDetail("INVALID_SENT_DATE", "Sent timestamp cannot be null"));
        }
        if (clock == null) {
            return Result.failure(new ErrorDetail("INVALID_OPERATION", "Clock supplier cannot be null"));
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

    // Getters
    public InvoiceId getId() { return id; }
    public BillingAccountId getBillingAccountId() { return billingAccountId; }
    public InvoiceNumber getInvoiceNumber() { return invoiceNumber; }
    public StripeInvoiceId getStripeInvoiceId() { return stripeInvoiceId; }
    public InvoiceStatus getStatus() { return status; }
    public Money getSubscriptionAmount() { return subscriptionAmount; }
    public Money getOverageAmount() { return overageAmount; }
    public Money getTotalAmount() { return totalAmount; }
    public BillingPeriod getBillingPeriod() { return billingPeriod; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getSentAt() { return sentAt; }
    public List<InvoiceLineItem> getLineItems() { return Collections.unmodifiableList(lineItems); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

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

    // Package-private setters for JPA/persistence layer
    void setId(InvoiceId id) { this.id = id; }
    void setBillingAccountId(BillingAccountId billingAccountId) { this.billingAccountId = billingAccountId; }
    void setInvoiceNumber(InvoiceNumber invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    void setStripeInvoiceId(StripeInvoiceId stripeInvoiceId) { this.stripeInvoiceId = stripeInvoiceId; }
    void setStatus(InvoiceStatus status) { this.status = status; }
    void setSubscriptionAmount(Money subscriptionAmount) { this.subscriptionAmount = subscriptionAmount; }
    void setOverageAmount(Money overageAmount) { this.overageAmount = overageAmount; }
    void setTotalAmount(Money totalAmount) { this.totalAmount = totalAmount; }
    void setBillingPeriod(BillingPeriod billingPeriod) { this.billingPeriod = billingPeriod; }
    void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    void setLineItems(List<InvoiceLineItem> lineItems) { this.lineItems = lineItems; }
    void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    void setVersion(long version) { this.version = version; }

    @Override
    public String toString() {
        return String.format("Invoice{id=%s, number=%s, status=%s, total=%s}", 
            id, invoiceNumber, status, totalAmount);
    }
}