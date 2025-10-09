package com.bcbs239.regtech.iam.infrastructure.outbox;

import com.bcbs239.regtech.core.events.GenericOutboxEventProcessor;
import com.bcbs239.regtech.core.events.OutboxEventPublisher;

/**
 * Concrete GenericOutboxEventProcessor for IAM bounded context.
 */
public class IamOutboxProcessor extends GenericOutboxEventProcessor {

    public IamOutboxProcessor(OutboxEventPublisher eventPublisher) {
        super(eventPublisher, "IAM");
    }

    @Override
    protected boolean isProcessingEnabled() {
        // For now enable processing; could be driven by config
        return true;
    }
}
