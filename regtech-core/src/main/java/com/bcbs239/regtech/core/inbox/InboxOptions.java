package com.bcbs239.regtech.core.inbox;

import java.time.Duration;

/**
 * Configuration options for the inbox processing job.
 */
public class InboxOptions {
    private final int batchSize;
    private final Duration pollInterval;
    private final String schema; // DB schema (for multi-schema setups)

    public InboxOptions(int batchSize, Duration pollInterval, String schema) {
        this.batchSize = batchSize;
        this.pollInterval = pollInterval;
        this.schema = schema;
    }

    public int getBatchSize() { return batchSize; }
    public Duration getPollInterval() { return pollInterval; }
    public String getSchema() { return schema; }
}