package com.bcbs239.regtech.core.application.inbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration options for the inbox processing job.
 * Binds to properties with prefix `inbox`, including `inbox.poll-interval` as a Duration.
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "inbox")
public class InboxOptions {
    private int batchSize = 10;
    private Duration pollInterval = Duration.ofSeconds(5);
    private boolean parallelProcessingEnabled = false;

    public InboxOptions() {
    }

}

