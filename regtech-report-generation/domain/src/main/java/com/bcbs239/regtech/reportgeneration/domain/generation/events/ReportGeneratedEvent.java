package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.Getter;

import java.time.Instant;

/**
 * Domain event raised when a report is successfully generated
 * Contains all metadata needed for downstream processing
 */
@Getter
public class ReportGeneratedEvent extends DomainEvent {
    
    private final ReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final ReportingDate reportingDate;
    private final PresignedUrl htmlPresignedUrl;
    private final PresignedUrl xbrlPresignedUrl;
    private final Instant generatedAt;
    
    public ReportGeneratedEvent(
            String correlationId,
            ReportId reportId,
            BatchId batchId,
            BankId bankId,
            ReportingDate reportingDate,
            PresignedUrl htmlPresignedUrl,
            PresignedUrl xbrlPresignedUrl,
            Instant generatedAt) {
        super(correlationId, "ReportGenerated");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.reportingDate = reportingDate;
        this.htmlPresignedUrl = htmlPresignedUrl;
        this.xbrlPresignedUrl = xbrlPresignedUrl;
        this.generatedAt = generatedAt;
    }
    
    @Override
    public String eventType() {
        return "ReportGenerated";
    }
}
