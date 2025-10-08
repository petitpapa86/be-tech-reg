package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import java.util.Objects;
import java.util.UUID;

/**
 * Invoice line item value object representing individual charges on an invoice
 */
public record InvoiceLineItem(
    InvoiceLineItemId id,
    String description,
    Money unitPrice,
    int quantity,
    Money totalAmount
) {
    
    public InvoiceLineItem {
        Objects.requireNonNull(id, "InvoiceLineItemId cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        Objects.requireNonNull(totalAmount, "Total amount cannot be null");
    }
    
    /**
     * Create a new invoice line item with validation
     */
    public static Result<InvoiceLineItem> create(String description, Money unitPrice, int quantity) {
        if (description == null || description.trim().isEmpty()) {
            return Result.failure(new ErrorDetail("INVALID_LINE_ITEM", "Description cannot be null or empty"));
        }
        if (unitPrice == null) {
            return Result.failure(new ErrorDetail("INVALID_LINE_ITEM", "Unit price cannot be null"));
        }
        if (quantity <= 0) {
            return Result.failure(new ErrorDetail("INVALID_LINE_ITEM", "Quantity must be positive"));
        }
        
        InvoiceLineItemId id = InvoiceLineItemId.generate();
        Money totalAmount = unitPrice.multiply(quantity);
        
        return Result.success(new InvoiceLineItem(id, description.trim(), unitPrice, quantity, totalAmount));
    }
    
    /**
     * Create a line item for subscription charges
     */
    public static Result<InvoiceLineItem> forSubscription(String tierName, Money monthlyAmount, BillingPeriod period) {
        String description = String.format("Subscription - %s (%s)", tierName, period.toString());
        return create(description, monthlyAmount, 1);
    }
    
    /**
     * Create a line item for overage charges
     */
    public static Result<InvoiceLineItem> forOverage(int overageCount, Money overageRate) {
        String description = String.format("Usage Overage (%d exposures)", overageCount);
        return create(description, overageRate, overageCount);
    }
    
    /**
     * Create a line item for pro-rated charges
     */
    public static Result<InvoiceLineItem> forProRatedSubscription(String tierName, Money proRatedAmount, BillingPeriod period) {
        String description = String.format("Pro-rated Subscription - %s (%s)", tierName, period.toString());
        return create(description, proRatedAmount, 1);
    }
}