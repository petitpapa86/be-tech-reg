package com.bcbs239.regtech.iam.infrastructure.inbox;

import com.bcbs239.regtech.core.events.GenericInboxEventProcessor;
import com.bcbs239.regtech.core.events.InboxEventPublisher;

/**
 * Concrete GenericInboxEventProcessor for IAM bounded context.
 */
public class IamInboxProcessor extends GenericInboxEventProcessor {

    public IamInboxProcessor(InboxEventPublisher eventPublisher) {
        super(eventPublisher, "IAM");
    }

    @Override
    protected boolean isProcessingEnabled() {
        // For now enable processing; could be driven by config
        return true;
    }
}