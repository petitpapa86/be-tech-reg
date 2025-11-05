package com.bcbs239.regtech.billing.domain.shared.events;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Domain representation of a webhook event from external payment provider
 */
public record WebhookEvent(
    String id,
    String type,
    JsonNode data,
    long created
) {
    
    /**
     * Get the object from the event data
     */
    public JsonNode getDataObject() {
        return data.has("object") ? data.get("object") : data;
    }
    
    /**
     * Check if this is an invoice-related event
     */
    public boolean isInvoiceEvent() {
        return type.startsWith("invoice.");
    }
    
    /**
     * Check if this is a subscription-related event
     */
    public boolean isSubscriptionEvent() {
        return type.startsWith("customer.subscription.");
    }
    
    /**
     * Check if this is a customer-related event
     */
    public boolean isCustomerEvent() {
        return type.startsWith("customer.");
    }
}

