package com.bcbs239.regtech.iam.infrastructure.events;

import com.bcbs239.regtech.core.events.GenericOutboxEventProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * IAM-specific outbox event processor that extends the generic implementation.
 * Provides IAM context configuration and conditional processing logic.
 */
@Component
@ConditionalOnProperty(name = "iam.outbox.enabled", havingValue = "true", matchIfMissing = false)
public class IamOutboxEventProcessor extends GenericOutboxEventProcessor {

    private final boolean processingEnabled;

    public IamOutboxEventProcessor(IamEventPublisher eventPublisher,
                                  @Value("${iam.outbox.enabled:false}") boolean processingEnabled,
                                  @Value("${iam.outbox.max-retries:3}") int maxRetries) {
        super(eventPublisher, "IAM", maxRetries);
        this.processingEnabled = processingEnabled;
    }

    @Override
    protected boolean isProcessingEnabled() {
        return processingEnabled;
    }
}