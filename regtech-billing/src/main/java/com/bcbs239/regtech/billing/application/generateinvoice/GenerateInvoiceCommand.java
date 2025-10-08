package com.bcbs239.regtech.billing.application.generateinvoice;

import com.bcbs239.regtech.billing.domain.valueobjects.BillingAccountId;
import com.bcbs239.regtech.billing.domain.valueobjects.BillingPeriod;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Command for manually generating an invoice for a billing account.
 * Contains billing account ID and billing period information.
 */
public record GenerateInvoiceCommand(
    @NotBlank(message = "Billing account ID is required")
    String billingAccountId,
    
    @NotNull(message = "Billing period is required")
    BillingPeriod billingPeriod
) {
    
    /**
     * Factory method to create and validate GenerateInvoiceCommand
     */
    public static Result<GenerateInvoiceCommand> create(String billingAccountId, BillingPeriod billingPeriod) {
        if (billingAccountId == null || billingAccountId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("BILLING_ACCOUNT_ID_REQUIRED", 
                "Billing account ID is required", "invoice.billing.account.id.required"));
        }
        
        if (billingPeriod == null) {
            return Result.failure(ErrorDetail.of("BILLING_PERIOD_REQUIRED", 
                "Billing period is required", "invoice.billing.period.required"));
        }
        
        return Result.success(new GenerateInvoiceCommand(
            billingAccountId.trim(),
            billingPeriod
        ));
    }
    
    /**
     * Factory method to create command for a specific month
     */
    public static Result<GenerateInvoiceCommand> forMonth(String billingAccountId, YearMonth yearMonth) {
        if (yearMonth == null) {
            return Result.failure(ErrorDetail.of("YEAR_MONTH_REQUIRED", 
                "Year month is required", "invoice.year.month.required"));
        }
        
        BillingPeriod period = BillingPeriod.forMonth(yearMonth);
        return create(billingAccountId, period);
    }
    
    /**
     * Factory method to create command for current month
     */
    public static Result<GenerateInvoiceCommand> forCurrentMonth(String billingAccountId) {
        return forMonth(billingAccountId, YearMonth.now());
    }
    
    /**
     * Get BillingAccountId value object
     */
    public Result<BillingAccountId> getBillingAccountId() {
        return BillingAccountId.fromString(billingAccountId);
    }
}