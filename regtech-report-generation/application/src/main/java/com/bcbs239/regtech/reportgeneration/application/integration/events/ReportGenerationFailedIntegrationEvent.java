package com.bcbs239.regtech.reportgeneration.application.integration.events;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Integration event published when report generation fails.
 * This event notifies downstream modules (notification service, monitoring)
 * that report generation encountered an error.
 * 
 * Requirements: 14.1
 * Event versioning: v1.0 - Initial version with failure information
 */
@Getter
public class ReportGenerationFailedIntegrationEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String reportId;
    private final String batchId;
    private final String bankId;
    private final String failureReason;
    private final String errorCode;
    private final Instant failedAt;
    private final String eventVersion;
    
    @JsonCreator
    public ReportGenerationFailedIntegrationEvent(
            @JsonProperty("reportId") String reportId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("failureReason") String failureReason,
            @JsonProperty("errorCode") String errorCode,
            @JsonProperty("failedAt") Instant failedAt) {
        super(batchId, Maybe.none(), "ReportGenerationFailedIntegrationEvent");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.failureReason = failureReason;
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
            "ReportGenerationFailedIntegrationEvent{reportId='%s', batchId='%s', bankId='%s', " +
            "failureReason='%s', errorCode='%s', failedAt=%s, version='%s'}",
            reportId, batchId, bankId, failureReason, errorCode, failedAt, eventVersion
        );
    }
}
