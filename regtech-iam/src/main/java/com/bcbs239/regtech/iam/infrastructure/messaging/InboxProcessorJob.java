package com.bcbs239.regtech.iam.infrastructure.messaging;

import com.bcbs239.regtech.core.inbox.ProcessInboxJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Inbox processor job that delegates to the generic ProcessInboxJob.
 * This implements the inbox pattern for reliable event processing in IAM context.
 */
@Component
public class InboxProcessorJob {

    private static final Logger logger = LoggerFactory.getLogger(InboxProcessorJob.class);

    private final ProcessInboxJob processInboxJob;

    public InboxProcessorJob(ProcessInboxJob processInboxJob) {
        this.processInboxJob = processInboxJob;
    }

    /**
     * Scheduled job to process inbox events every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void processInboxEvents() {
        logger.debug("Starting IAM inbox event processing");
        int processed = processInboxJob.runOnce();
        logger.debug("Completed IAM inbox event processing: {} events processed", processed);
    }

    /**
     * Manually trigger inbox processing (for testing or admin purposes).
     */
    public void runOnce() {
        logger.info("Manually triggering IAM inbox processing");
        int processed = processInboxJob.runOnce();
        logger.info("Manually processed {} IAM inbox events", processed);
    }
}