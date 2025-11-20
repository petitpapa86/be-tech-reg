package com.bcbs239.regtech.core.application.outbox;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Setter
@Getter
public class OutboxOptions {
    private int batchSize = 10;
    private Duration pollInterval = Duration.ofSeconds(30);
    private boolean parallelProcessingEnabled = false;

    public OutboxOptions() {
    }

}
