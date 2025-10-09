package com.bcbs239.regtech.core.outbox;

import com.bcbs239.regtech.core.events.OutboxEventPublisher;

/**
 * Adapter to expose ProcessOutboxJob as an OutboxEventPublisher so we can retire the older API
 */
public class ProcessOutboxEventPublisher implements OutboxEventPublisher {

    private final ProcessOutboxJob job;

    public ProcessOutboxEventPublisher(ProcessOutboxJob job) {
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
    public OutboxEventStats getStats() {
        var s = job.stats();
        return new OutboxEventStats(s.pending(), s.processing(), s.processed(), s.failed(), s.deadLetter());
    }
}
