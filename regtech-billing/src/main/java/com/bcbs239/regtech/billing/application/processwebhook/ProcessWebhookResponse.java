package com.bcbs239.regtech.billing.application.processwebhook;

/**
 * Response for webhook processing.
 * Contains the processing result and any relevant information.
 */
public record ProcessWebhookResponse(
    String eventId,
    String eventType,
    ProcessingResult result,
    String message,
    boolean wasAlreadyProcessed
) {
    
    public enum ProcessingResult {
        SUCCESS,
        ALREADY_PROCESSED,
        IGNORED,
        FAILED
    }
    
    /**
     * Factory method for successful processing
     */
    public static ProcessWebhookResponse success(String eventId, String eventType, String message) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingResult.SUCCESS, message, false);
    }
    
    /**
     * Factory method for already processed events
     */
    public static ProcessWebhookResponse alreadyProcessed(String eventId, String eventType) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingResult.ALREADY_PROCESSED, 
            "Event was already processed", true);
    }
    
    /**
     * Factory method for ignored events
     */
    public static ProcessWebhookResponse ignored(String eventId, String eventType, String reason) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingResult.IGNORED, reason, false);
    }
    
    /**
     * Factory method for failed processing
     */
    public static ProcessWebhookResponse failed(String eventId, String eventType, String error) {
        return new ProcessWebhookResponse(eventId, eventType, ProcessingResult.FAILED, error, false);
    }
    
    /**
     * Check if processing was successful
     */
    public boolean isSuccess() {
        return result == ProcessingResult.SUCCESS || result == ProcessingResult.ALREADY_PROCESSED;
    }
}
