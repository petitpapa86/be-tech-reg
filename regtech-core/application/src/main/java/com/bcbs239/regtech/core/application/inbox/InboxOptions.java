package com.bcbs239.regtech.core.application.inbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration options for the inbox processing job.
 * Binds to properties with prefix `inbox`, including `inbox.poll-interval` as a Duration.
 */
@ConfigurationProperties(prefix = "inbox")
public class InboxOptions {
    private int batchSize = 10;
    private Duration pollInterval = Duration.ofSeconds(5);
    private boolean parallelProcessingEnabled = false;

    public InboxOptions() {
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public boolean isParallelProcessingEnabled() {
        return parallelProcessingEnabled;
    }

    public void setParallelProcessingEnabled(boolean parallelProcessingEnabled) {
        this.parallelProcessingEnabled = parallelProcessingEnabled;
    }

}

