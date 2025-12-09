package com.bcbs239.regtech.billing.application.payments;


import com.bcbs239.regtech.billing.domain.payments.PaymentMethodId;
import com.bcbs239.regtech.billing.domain.shared.BillingValidationUtils;
import com.bcbs239.regtech.core.domain.shared.Result;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command for processing payment information during user registration.
 * Contains payment method ID and correlation ID from the registration saga.
 */
public record ProcessPaymentCommand(
    @NotBlank(message = "Payment method ID is required")

    String paymentMethodId,
    
    @NotBlank(message = "Correlation ID is required")
    @Size(min = 3, max = 100, message = "Correlation ID must be between 3 and 100 characters")
    String correlationId
) {
    
    /**
     * Factory method to create and validate ProcessPaymentCommand
     */
    public static Result<ProcessPaymentCommand> create(String paymentMethodId, String correlationId) {
        // Sanitize inputs
        String sanitizedPaymentMethodId = BillingValidationUtils.sanitizeStringInput(paymentMethodId);
        String sanitizedCorrelationId = BillingValidationUtils.sanitizeStringInput(correlationId);
        
        // Validate payment method ID
        Result<Void> paymentMethodValidation = BillingValidationUtils.validateStripePaymentMethodId(sanitizedPaymentMethodId);
        if (paymentMethodValidation.isFailure()) {
            return Result.failure(paymentMethodValidation.getError().get());
        }
        
        // Validate correlation ID
        Result<Void> correlationIdValidation = BillingValidationUtils.validateCorrelationId(sanitizedCorrelationId);
        if (correlationIdValidation.isFailure()) {
            return Result.failure(correlationIdValidation.getError().get());
        }
        
        return Result.success(new ProcessPaymentCommand(
            sanitizedPaymentMethodId,
            sanitizedCorrelationId
        ));
    }
    
    /**
     * Get PaymentMethodId value object
     */
    public PaymentMethodId getPaymentMethodId() {
        return PaymentMethodId.fromString(paymentMethodId).getValue().get();
    }
}

