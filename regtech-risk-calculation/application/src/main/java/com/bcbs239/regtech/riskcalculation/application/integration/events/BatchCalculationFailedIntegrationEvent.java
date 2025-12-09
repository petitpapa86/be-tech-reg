package com.bcbs239.regtech.riskcalculation.application.integration.events;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import lombok.Getter;

import java.time.Instant;

/**
 * Integration event published when risk calculation fails.
 * This event notifies downstream modules and support teams about calculation failures
 * that require attention or alternative processing.
 * 
 * Event versioning: v1.0 - Initial version with error details
 */
@Getter
public class BatchCalculationFailedIntegrationEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String batchId;
    private final String bankId;
    private final String errorMessage;
    private final String errorCode;
    private final Instant failedAt;
    private final String eventVersion;
    
    public BatchCalculationFailedIntegrationEvent(
            String batchId,
            String bankId,
            String errorMessage,
            String errorCode,
            Instant failedAt) {
        
        super(batchId, Maybe.none(), "BatchCalculationFailedIntegrationEvent");
        
        this.batchId = batchId;
        this.bankId = bankId;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.failedAt = failedAt;
        this.eventVersion = EVENT_VERSION;
    }
    
    @Override
    public String eventType() {
        return getEventType();
    }
    
    @Override
    public String toString() {
        return String.format(
            "BatchCalculationFailedIntegrationEvent{batchId='%s', bankId='%s', errorCode='%s', " +
            "errorMessage='%s', failedAt=%s, version='%s'}",
            batchId, bankId, errorCode, errorMessage, failedAt, eventVersion
        );
    }
}