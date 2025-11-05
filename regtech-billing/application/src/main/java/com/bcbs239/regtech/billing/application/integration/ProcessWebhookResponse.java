package com.bcbs239.regtech.billing.application.integration;

/**
 * Response for webhook processing operations
 */
public record ProcessWebhookResponse(
    String eventId,
    String eventType,
    ProcessingStatus status,
    String message
) {
    
    public enum ProcessingStatus {
        PROCESSED,
        IGNORED,
        ALREADY_PROCESSED
    }
    
    public static ProcessWebhookResponse processed(String eventId, String eventType) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingStatus.PROCESSED, 
            "Webhook event processed successfully");
    }
    
    public static ProcessWebhookResponse ignored(String eventId, String eventType) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingStatus.IGNORED, 
            "Webhook event type not supported or not relevant");
    }
    
    public static ProcessWebhookResponse alreadyProcessed(String eventId, String eventType) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingStatus.ALREADY_PROCESSED, 
            "Webhook event was already processed");
    }
}

