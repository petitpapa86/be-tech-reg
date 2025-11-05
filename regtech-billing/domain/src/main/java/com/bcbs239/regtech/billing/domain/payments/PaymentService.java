package com.bcbs239.regtech.billing.domain.payments;

import com.bcbs239.regtech.billing.domain.subscriptions.SubscriptionTier;
import com.bcbs239.regtech.core.shared.Result;

/**
 * Domain service interface for payment processing operations.
 * Abstracts external payment provider (Stripe) from the application layer.
 */
public interface PaymentService {
    
    /**
     * Creates a customer in the payment system
     */
    Result<CustomerCreationResult> createCustomer(CustomerCreationRequest request);
    
    /**
     * Creates a subscription for a customer
     */
    Result<SubscriptionCreationResult> createSubscription(SubscriptionCreationRequest request);
    
    /**
     * Cancels a subscription
     */
    Result<Void> cancelSubscription(String subscriptionId);
    
    /**
     * Creates an invoice for a customer
     */
    Result<InvoiceCreationResult> createInvoice(InvoiceCreationRequest request);
    
    /**
     * Verifies webhook signature and parses event
     */
    Result<com.bcbs239.regtech.billing.domain.shared.events.WebhookEvent> verifyAndParseWebhook(String payload, String signatureHeader);
    
    /**
     * Request for creating a customer
     */
    record CustomerCreationRequest(
        String email,
        String name,
        PaymentMethodId paymentMethodId
    ) {}
    
    /**
     * Result of customer creation
     */
    record CustomerCreationResult(
        StripeCustomerId customerId,
        String email,
        String name
    ) {}
    
    /**
     * Request for creating a subscription
     */
    record SubscriptionCreationRequest(
        StripeCustomerId customerId,
        SubscriptionTier tier,
        PaymentMethodId paymentMethodId
    ) {}
    
    /**
     * Result of subscription creation
     */
    record SubscriptionCreationResult(
        String subscriptionId,
        String status,
        StripeCustomerId customerId
    ) {}
    
    /**
     * Request for creating an invoice
     */
    record InvoiceCreationRequest(
        StripeCustomerId customerId,
        String amount,
        String description
    ) {}
    
    /**
     * Result of invoice creation
     */
    record InvoiceCreationResult(
        String invoiceId,
        String status,
        String amount
    ) {}
}

