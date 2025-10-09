package com.bcbs239.regtech.billing.domain.invoices;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;

/**
 * Value object representing an invoice line item.
 * Contains details about a specific charge on an invoice.
 */
public record InvoiceLineItem(
    InvoiceLineItemId id,
    String description,
    Money unitAmount,
    int quantity,
    Money totalAmount
) {

    /**
     * Create a subscription line item for the given tier and amount.
     */
    public static Result<InvoiceLineItem> forSubscription(String tier, Money amount, BillingPeriod billingPeriod) {
        if (tier == null || tier.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("INVALID_TIER", "Tier cannot be null or empty", "invoice.lineitem.tier.invalid"));
        }
        if (amount == null) {
            return Result.failure(ErrorDetail.of("INVALID_AMOUNT", "Amount cannot be null", "invoice.lineitem.amount.invalid"));
        }
        if (!amount.isPositive()) {
            return Result.failure(ErrorDetail.of("INVALID_AMOUNT", "Amount must be positive", "invoice.lineitem.amount.negative"));
        }

        InvoiceLineItemId id = InvoiceLineItemId.generate();
        String description = String.format("%s subscription for %s", tier, billingPeriod.toString());
        Money unitAmount = amount;
        int quantity = 1;
        Money totalAmount = amount;

        return Result.success(new InvoiceLineItem(id, description, unitAmount, quantity, totalAmount));
    }

    /**
     * Create an overage line item for the given count and rate.
     */
    public static Result<InvoiceLineItem> forOverage(int overageCount, Money overageRate) {
        if (overageCount <= 0) {
            return Result.failure(ErrorDetail.of("INVALID_COUNT", "Overage count must be positive", "invoice.lineitem.count.invalid"));
        }
        if (overageRate == null) {
            return Result.failure(ErrorDetail.of("INVALID_RATE", "Overage rate cannot be null", "invoice.lineitem.rate.invalid"));
        }
        if (!overageRate.isPositive()) {
            return Result.failure(ErrorDetail.of("INVALID_RATE", "Overage rate must be positive", "invoice.lineitem.rate.negative"));
        }

        InvoiceLineItemId id = InvoiceLineItemId.generate();
        String description = String.format("Overage charges for %d additional exposures", overageCount);
        Money unitAmount = overageRate;
        int quantity = overageCount;
        Money totalAmount = overageRate.multiply(overageCount);

        return Result.success(new InvoiceLineItem(id, description, unitAmount, quantity, totalAmount));
    }
}
