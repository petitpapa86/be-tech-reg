package com.bcbs239.regtech.billing.api;

import com.bcbs239.regtech.billing.application.processpayment.ProcessPaymentCommand;
import com.bcbs239.regtech.billing.application.processpayment.ProcessPaymentCommandHandler;
import com.bcbs239.regtech.billing.application.processpayment.ProcessPaymentResponse;
import com.bcbs239.regtech.core.shared.ApiResponse;
import com.bcbs239.regtech.core.shared.BaseController;
import com.bcbs239.regtech.core.shared.Result;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for billing operations including payment processing.
 * Handles payment information collection and processing during user registration.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingController extends BaseController {

    private final ProcessPaymentCommandHandler processPaymentCommandHandler;

    public BillingController(ProcessPaymentCommandHandler processPaymentCommandHandler) {
        this.processPaymentCommandHandler = processPaymentCommandHandler;
    }

    /**
     * Process payment information during user registration.
     * Creates Stripe customer, billing account, subscription, and first invoice.
     *
     * @param request The payment processing request
     * @return ResponseEntity with ProcessPaymentResponse or error details
     */
    @PostMapping("/process-payment")
    public ResponseEntity<? extends ApiResponse<?>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        
        try {
            // Create and validate command
            Result<ProcessPaymentCommand> commandResult = ProcessPaymentCommand.create(
                request.paymentMethodId(),
                request.correlationId()
            );
            
            if (commandResult.isFailure()) {
                return handleError(commandResult.getError().get());
            }
            
            // Execute command
            Result<ProcessPaymentResponse> result = processPaymentCommandHandler.handle(
                commandResult.getValue().get()
            );
            
            return handleResult(result, 
                "Payment processed successfully", 
                "billing.payment.processed");
                
        } catch (Exception e) {
            return handleSystemError(e);
        }
    }

    /**
     * Request DTO for payment processing endpoint
     */
    public record ProcessPaymentRequest(
        @jakarta.validation.constraints.NotBlank(message = "Payment method ID is required")
        @com.bcbs239.regtech.billing.infrastructure.validation.ValidStripeId(
            type = com.bcbs239.regtech.billing.infrastructure.validation.ValidStripeId.StripeIdType.PAYMENT_METHOD,
            message = "Invalid payment method ID format"
        )
        String paymentMethodId,
        
        @jakarta.validation.constraints.NotBlank(message = "Correlation ID is required")
        @jakarta.validation.constraints.Size(min = 3, max = 100, message = "Correlation ID must be between 3 and 100 characters")
        String correlationId
    ) {
        
        /**
         * Factory method to create and validate ProcessPaymentRequest
         */
        public static Result<ProcessPaymentRequest> create(String paymentMethodId, String correlationId) {
            // Sanitize inputs
            String sanitizedPaymentMethodId = com.bcbs239.regtech.billing.infrastructure.validation.BillingValidationUtils.sanitizeStringInput(paymentMethodId);
            String sanitizedCorrelationId = com.bcbs239.regtech.billing.infrastructure.validation.BillingValidationUtils.sanitizeStringInput(correlationId);
            
            // Validate payment method ID
            Result<Void> paymentMethodValidation = com.bcbs239.regtech.billing.infrastructure.validation.BillingValidationUtils.validateStripePaymentMethodId(sanitizedPaymentMethodId);
            if (paymentMethodValidation.isFailure()) {
                return Result.failure(paymentMethodValidation.getError().get());
            }
            
            // Validate correlation ID
            Result<Void> correlationIdValidation = com.bcbs239.regtech.billing.infrastructure.validation.BillingValidationUtils.validateCorrelationId(sanitizedCorrelationId);
            if (correlationIdValidation.isFailure()) {
                return Result.failure(correlationIdValidation.getError().get());
            }
            
            return Result.success(new ProcessPaymentRequest(
                sanitizedPaymentMethodId,
                sanitizedCorrelationId
            ));
        }
    }
}