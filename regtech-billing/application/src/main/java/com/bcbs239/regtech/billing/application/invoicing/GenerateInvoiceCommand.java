package com.bcbs239.regtech.billing.application.invoicing;


import com.bcbs239.regtech.billing.domain.accounts.BillingAccountId;
import com.bcbs239.regtech.billing.domain.shared.validation.BillingValidationUtils;
import com.bcbs239.regtech.billing.domain.shared.valueobjects.BillingPeriod;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.YearMonth;

/**
 * Command for manually generating an invoice for a billing account.
 * Contains billing account ID and billing period information.
 */
public record GenerateInvoiceCommand(
    @NotBlank(message = "Billing account ID is required")
    @Size(min = 3, max = 50, message = "Billing account ID must be between 3 and 50 characters")
    String billingAccountId,
    
    @NotNull(message = "Billing period is required")
    BillingPeriod billingPeriod
) {
    
    /**
     * Factory method to create and validate GenerateInvoiceCommand
     */
    public static Result<GenerateInvoiceCommand> create(String billingAccountId, BillingPeriod billingPeriod) {
        // Sanitize input
        String sanitizedBillingAccountId = BillingValidationUtils.sanitizeStringInput(billingAccountId);
        
        // Validate billing account ID
        Result<Void> billingAccountIdValidation = BillingValidationUtils.validateBillingAccountId(sanitizedBillingAccountId);
        if (billingAccountIdValidation.isFailure()) {
            return Result.failure(billingAccountIdValidation.getError().get());
        }
        
        if (billingPeriod == null) {
            return Result.failure(ErrorDetail.of("BILLING_PERIOD_REQUIRED", 
                "Billing period is required", "invoice.billing.period.required"));
        }
        
        // Validate billing period dates
        if (billingPeriod.getStartDate().isAfter(billingPeriod.getEndDate())) {
            return Result.failure(ErrorDetail.of("INVALID_BILLING_PERIOD", 
                "Billing period start date cannot be after end date", "invoice.billing.period.invalid"));
        }
        
        return Result.success(new GenerateInvoiceCommand(
            sanitizedBillingAccountId,
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

