package com.bcbs239.regtech.core.outbox;

import java.time.Duration;

/**
 * Configuration options for the outbox processing job.
 */
public class OutboxOptions {
    private final int batchSize;
    private final Duration pollInterval;
    private final String schema; // DB schema (for multi-schema setups)
    private final boolean parallelProcessingEnabled;

    public OutboxOptions(int batchSize, Duration pollInterval, String schema) {
        this(batchSize, pollInterval, schema, false);
    }

    public OutboxOptions(int batchSize, Duration pollInterval, String schema, boolean parallelProcessingEnabled) {
        this.batchSize = batchSize;
        this.pollInterval = pollInterval;
        this.schema = schema;
        this.parallelProcessingEnabled = parallelProcessingEnabled;
    }

    public int getBatchSize() { return batchSize; }
    public Duration getPollInterval() { return pollInterval; }
    public String getSchema() { return schema; }
    public boolean isParallelProcessingEnabled() { return parallelProcessingEnabled; }
}
