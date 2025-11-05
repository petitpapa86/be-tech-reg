package com.bcbs239.regtech.core.application.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "outbox")
public class OutboxOptions {
    private int batchSize = 10;
    private Duration pollInterval = Duration.ofSeconds(30);
    private boolean parallelProcessingEnabled = false;

    public OutboxOptions() {
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

