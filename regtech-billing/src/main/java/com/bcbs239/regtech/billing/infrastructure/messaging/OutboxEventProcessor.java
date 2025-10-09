package com.bcbs239.regtech.billing.infrastructure.messaging;

import com.bcbs239.regtech.core.events.GenericOutboxEventProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Billing-specific outbox event processor that extends the generic implementation.
 * Provides billing context configuration and conditional processing logic.
 */
@Component
@ConditionalOnProperty(name = "billing.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventProcessor extends GenericOutboxEventProcessor {

    private final boolean processingEnabled;

    public OutboxEventProcessor(BillingEventPublisher eventPublisher,
                               @Value("${billing.outbox.enabled:true}") boolean processingEnabled,
                               @Value("${billing.outbox.max-retries:3}") int maxRetries) {
        super(eventPublisher, "Billing", maxRetries);
        this.processingEnabled = processingEnabled;
    }

    @Override
    protected boolean isProcessingEnabled() {
        return processingEnabled;
    }
}
