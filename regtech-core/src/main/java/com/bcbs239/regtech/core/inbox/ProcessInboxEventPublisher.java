package com.bcbs239.regtech.core.inbox;

import com.bcbs239.regtech.core.events.InboxEventPublisher;

/**
 * Adapter to expose ProcessInboxJob as an InboxEventPublisher so we can retire the older API
 */
public class ProcessInboxEventPublisher implements InboxEventPublisher {

    private final ProcessInboxJob job;

    public ProcessInboxEventPublisher(ProcessInboxJob job) {
        this.job = job;
    }

    @Override
    public void processPendingEvents() {
        job.runOnce();
    }

    @Override
    public void retryFailedEvents(int maxRetries) {
        // Attempt to load failed messages and process them. We don't currently enforce maxRetries here —
        // repositories should filter messages eligible for retry.
        var failed = job.repository().failedMessageLoader().apply(job.options().getBatchSize());
        job.processMessages(failed);
    }

    @Override
    public InboxEventPublisher.InboxEventStats getStats() {
        var s = job.stats();
        return new InboxEventPublisher.InboxEventStats(s.pending(), s.processing(), s.processed(), s.failed(), s.deadLetter());
    }
}