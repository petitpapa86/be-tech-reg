package com.bcbs239.regtech.core.application.eventprocessing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.event.retry")
public class EventRetryOptions {
    private Duration interval = Duration.ofMillis(60000); // Check every 60 seconds
    private int batchSize = 10; // Process 10 events per batch
    private int maxRetries = 5; // Maximum retry attempts
    private boolean enabled = true; // Enable/disable retry processing
    
    // Backoff intervals in seconds: [10s, 30s, 1min, 5min, 10min]
    private long[] backoffIntervalsSeconds = {10, 30, 60, 300, 600};

    public EventRetryOptions() {
    }
    
    public long[] getBackoffIntervalsSeconds() {
        return backoffIntervalsSeconds;
    }
}