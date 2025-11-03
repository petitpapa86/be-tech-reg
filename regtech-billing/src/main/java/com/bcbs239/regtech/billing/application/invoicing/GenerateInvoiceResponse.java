package com.bcbs239.regtech.billing.application.invoicing;

import com.bcbs239.regtech.billing.domain.invoices.InvoiceId;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceNumber;
import com.bcbs239.regtech.billing.domain.invoices.InvoiceStatus;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import java.time.LocalDate;

/**
 * Response for successful invoice generation.
 * Contains the generated invoice details and amounts.
 */
public record GenerateInvoiceResponse(
    InvoiceId invoiceId,
    InvoiceNumber invoiceNumber,
    InvoiceStatus status,
    Money subscriptionAmount,
    Money overageAmount,
    Money totalAmount,
    BillingPeriod billingPeriod,
    LocalDate issueDate,
    LocalDate dueDate,
    int actualUsage,
    int usageLimit
) {
    
    /**
     * Factory method to create GenerateInvoiceResponse
     */
    public static GenerateInvoiceResponse of(
            InvoiceId invoiceId,
            InvoiceNumber invoiceNumber,
            InvoiceStatus status,
            Money subscriptionAmount,
            Money overageAmount,
            Money totalAmount,
            BillingPeriod billingPeriod,
            LocalDate issueDate,
            LocalDate dueDate,
            int actualUsage,
            int usageLimit) {
        return new GenerateInvoiceResponse(
            invoiceId,
            invoiceNumber,
            status,
            subscriptionAmount,
            overageAmount,
            totalAmount,
            billingPeriod,
            issueDate,
            dueDate,
            actualUsage,
            usageLimit
        );
    }
    
    /**
     * Check if this invoice has overage charges
     */
    public boolean hasOverageCharges() {
        return overageAmount.isPositive();
    }
    
    /**
     * Check if usage exceeded the limit
     */
    public boolean isUsageOverLimit() {
        return actualUsage > usageLimit;
    }
}
