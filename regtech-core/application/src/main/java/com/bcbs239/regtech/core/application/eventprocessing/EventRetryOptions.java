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

    public EventRetryOptions() {
    }
}