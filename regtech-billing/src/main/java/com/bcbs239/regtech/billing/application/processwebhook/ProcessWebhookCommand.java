package com.bcbs239.regtech.billing.application.processwebhook;

import com.bcbs239.regtech.billing.infrastructure.validation.BillingValidationUtils;
import com.bcbs239.regtech.billing.infrastructure.validation.ValidStripeId;
import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command for processing Stripe webhook events.
 * Contains the webhook payload and signature for verification.
 */
public record ProcessWebhookCommand(
    @NotBlank(message = "Event ID is required")
    @ValidStripeId(type = ValidStripeId.StripeIdType.EVENT, message = "Invalid Stripe event ID format")
    String eventId,
    
    @NotBlank(message = "Event type is required")
    @Size(min = 3, max = 100, message = "Event type must be between 3 and 100 characters")
    String eventType,
    
    @NotBlank(message = "Payload is required")
    @Size(min = 10, max = 50000, message = "Payload must be between 10 and 50000 characters")
    String payload,
    
    @NotBlank(message = "Signature header is required")
    @Size(min = 10, max = 500, message = "Signature header must be between 10 and 500 characters")
    String signatureHeader
) {
    
    /**
     * Factory method to create and validate ProcessWebhookCommand
     */
    public static Result<ProcessWebhookCommand> create(
            String eventId, 
            String eventType, 
            String payload, 
            String signatureHeader) {
        
        // Sanitize inputs (except payload which should remain unchanged)
        String sanitizedEventId = BillingValidationUtils.sanitizeStringInput(eventId);
        String sanitizedEventType = BillingValidationUtils.sanitizeStringInput(eventType);
        String sanitizedSignatureHeader = BillingValidationUtils.sanitizeStringInput(signatureHeader);
        
        // Validate event ID
        Result<Void> eventIdValidation = BillingValidationUtils.validateStripeEventId(sanitizedEventId);
        if (eventIdValidation.isFailure()) {
            return Result.failure(eventIdValidation.getError().get());
        }
        
        // Validate event type
        if (sanitizedEventType == null || sanitizedEventType.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("EVENT_TYPE_REQUIRED", 
                "Event type is required", "webhook.event.type.required"));
        }
        
        // Validate webhook payload structure
        Result<JsonNode> payloadValidation = BillingValidationUtils.validateWebhookPayload(payload);
        if (payloadValidation.isFailure()) {
            return Result.failure(payloadValidation.getError().get());
        }
        
        // Validate signature header format
        Result<Void> signatureValidation = BillingValidationUtils.validateWebhookSignature(sanitizedSignatureHeader);
        if (signatureValidation.isFailure()) {
            return Result.failure(signatureValidation.getError().get());
        }
        
        return Result.success(new ProcessWebhookCommand(
            sanitizedEventId,
            sanitizedEventType,
            payload, // Keep original payload for signature verification
            sanitizedSignatureHeader
        ));
    }
    
    /**
     * Check if this is an invoice-related event
     */
    public boolean isInvoiceEvent() {
        return eventType.startsWith("invoice.") || 
               eventType.equals("payment_intent.succeeded") ||
               eventType.equals("payment_intent.payment_failed");
    }
    
    /**
     * Check if this is a subscription-related event
     */
    public boolean isSubscriptionEvent() {
        return eventType.startsWith("customer.subscription.");
    }
    
    /**
     * Check if this is a payment-related event
     */
    public boolean isPaymentEvent() {
        return eventType.startsWith("payment_intent.") ||
               eventType.startsWith("payment_method.");
    }
}