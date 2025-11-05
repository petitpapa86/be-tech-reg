package com.bcbs239.regtech.core.application.inbox;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

/**
 * Configuration options for the inbox processing job.
 */
@ConfigurationProperties(prefix = "inbox")
@ConstructorBinding
public class InboxOptions {
    private final int batchSize;
    private final Duration pollInterval;
    private final boolean parallelProcessingEnabled;

    public InboxOptions(int batchSize, Duration pollInterval, boolean parallelProcessingEnabled) {
        this.batchSize = batchSize;
        this.pollInterval = pollInterval;
        this.parallelProcessingEnabled = parallelProcessingEnabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public boolean isParallelProcessingEnabled() {
        return parallelProcessingEnabled;
    }

}
