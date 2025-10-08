package com.bcbs239.regtech.billing.application.processwebhook;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.core.shared.ErrorDetail;
import jakarta.validation.constraints.NotBlank;

/**
 * Command for processing Stripe webhook events.
 * Contains the webhook payload and signature for verification.
 */
public record ProcessWebhookCommand(
    @NotBlank(message = "Event ID is required")
    String eventId,
    
    @NotBlank(message = "Event type is required")
    String eventType,
    
    @NotBlank(message = "Payload is required")
    String payload,
    
    @NotBlank(message = "Signature header is required")
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
        
        if (eventId == null || eventId.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("EVENT_ID_REQUIRED", 
                "Event ID is required", "webhook.event.id.required"));
        }
        
        if (eventType == null || eventType.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("EVENT_TYPE_REQUIRED", 
                "Event type is required", "webhook.event.type.required"));
        }
        
        if (payload == null || payload.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("PAYLOAD_REQUIRED", 
                "Payload is required", "webhook.payload.required"));
        }
        
        if (signatureHeader == null || signatureHeader.trim().isEmpty()) {
            return Result.failure(ErrorDetail.of("SIGNATURE_HEADER_REQUIRED", 
                "Signature header is required", "webhook.signature.header.required"));
        }
        
        return Result.success(new ProcessWebhookCommand(
            eventId.trim(),
            eventType.trim(),
            payload,
            signatureHeader.trim()
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