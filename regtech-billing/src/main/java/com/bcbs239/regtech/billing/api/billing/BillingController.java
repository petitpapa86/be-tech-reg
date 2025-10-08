package com.bcbs239.regtech.billing.api;

import com.bcbs239.regtech.billing.application.commands.ProcessPaymentCommand;
import com.bcbs239.regtech.billing.application.commands.ProcessPaymentCommandHandler;
import com.bcbs239.regtech.billing.application.commands.ProcessPaymentResponse;
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
        String paymentMethodId,
        
        @jakarta.validation.constraints.NotBlank(message = "Correlation ID is required")
        String correlationId
    ) {}
}