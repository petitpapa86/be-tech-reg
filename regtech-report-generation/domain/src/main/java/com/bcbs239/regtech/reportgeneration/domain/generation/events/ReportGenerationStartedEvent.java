package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when report generation starts
 * Signals the beginning of the report generation process
 * 
 * Requirements: 14.1
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportGenerationStartedEvent extends DomainEvent {
    
    private final ReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final Instant startedAt;
    
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReportGenerationStartedEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("reportId") ReportId reportId,
            @JsonProperty("batchId") BatchId batchId,
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("startedAt") Instant startedAt) {
        super(correlationId);
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.startedAt = startedAt;
    }
}
