package com.bcbs239.regtech.billing.application.commands;

import com.bcbs239.regtech.billing.domain.valueobjects.PaymentMethodId;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Command for processing payment information during user registration.
 * Contains payment method ID and correlation ID from the registration saga.
 */
public record ProcessPaymentCommand(
    @NotBlank(message = "Payment method ID is required")
    String paymentMethodId,
    
    @NotBlank(message = "Correlation ID is required")
    String correlationId
) {
    
    /**
     * Factory method to create and validate ProcessPaymentCommand
     */
    public static Result<ProcessPaymentCommand> create(String paymentMethodId, String correlationId) {
        if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("PAYMENT_METHOD_ID_REQUIRED", 
                "Payment method ID is required", "payment.method.id.required"));
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("CORRELATION_ID_REQUIRED", 
                "Correlation ID is required", "payment.correlation.id.required"));
        }
        
        return Result.success(new ProcessPaymentCommand(
            paymentMethodId.trim(),
            correlationId.trim()
        ));
    }
    
    /**
     * Get PaymentMethodId value object
     */
    public PaymentMethodId getPaymentMethodId() {
        return PaymentMethodId.fromString(paymentMethodId).getValue().get();
    }
}