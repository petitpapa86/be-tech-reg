package com.bcbs239.regtech.billing.api.dto;

import com.bcbs239.regtech.billing.domain.valueobjects.Money;
import com.bcbs239.regtech.billing.infrastructure.validation.BillingValidationUtils;
import com.bcbs239.regtech.billing.infrastructure.validation.ValidCurrency;
import com.bcbs239.regtech.billing.infrastructure.validation.ValidPaymentAmount;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Request DTO for payment amounts with currency validation.
 * Used in API endpoints that accept payment amount information.
 */
public record PaymentAmountRequest(
    @NotNull(message = "Payment amount is required")
    @ValidPaymentAmount
    BigDecimal amount,
    
    @NotBlank(message = "Currency code is required")
    @ValidCurrency
    String currencyCode
) {
    
    /**
     * Factory method to create and validate PaymentAmountRequest
     */
    public static Result<PaymentAmountRequest> create(BigDecimal amount, String currencyCode) {
        // Validate payment amount
        Result<Void> amountValidation = BillingValidationUtils.validatePaymentAmount(amount);
        if (amountValidation.isFailure()) {
            return Result.failure(amountValidation.getError().get());
        }
        
        // Validate and normalize currency
        Result<Currency> currencyValidation = BillingValidationUtils.validateCurrency(currencyCode);
        if (currencyValidation.isFailure()) {
            return Result.failure(currencyValidation.getError().get());
        }
        
        Currency currency = currencyValidation.getValue().get();
        
        return Result.success(new PaymentAmountRequest(
            amount,
            currency.getCurrencyCode()
        ));
    }
    
    /**
     * Convert to Money value object
     */
    public Result<Money> toMoney() {
        Result<Currency> currencyResult = BillingValidationUtils.validateCurrency(currencyCode);
        if (currencyResult.isFailure()) {
            return Result.failure(currencyResult.getError().get());
        }
        
        return Result.success(Money.of(amount, currencyResult.getValue().get()));
    }
    
    /**
     * Get normalized currency code
     */
    public String getNormalizedCurrencyCode() {
        return currencyCode.toUpperCase().trim();
    }
}