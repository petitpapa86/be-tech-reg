package com.bcbs239.regtech.billing.infrastructure.inbox;

import com.bcbs239.regtech.core.events.GenericInboxEventProcessor;
import com.bcbs239.regtech.core.events.InboxEventPublisher;

/**
 * Concrete GenericInboxEventProcessor for Billing bounded context.
 * Processes integration events from other bounded contexts.
 */
public class BillingInboxProcessor extends GenericInboxEventProcessor {

    public BillingInboxProcessor(InboxEventPublisher eventPublisher) {
        super(eventPublisher, "BILLING");
    }

    @Override
    protected boolean isProcessingEnabled() {
        // For now enable processing; could be driven by config
        return true;
    }

    /**
     * Manually trigger inbox processing for testing purposes.
     */
    public void processPendingEventsManually() {
        super.processPendingEvents();
    }
}