package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Domain event raised when a report is successfully generated
 * Contains all metadata needed for downstream processing and notifications
 * 
 * Requirements: 14.1, 15.1, 15.2
 */
@Getter
public class ReportGeneratedEvent extends DomainEvent {
    
    private final ReportId reportId;
    private final BatchId batchId;
    private final BankId bankId;
    private final ReportType reportType;
    private final ReportingDate reportingDate;
    
    // S3 URIs for file locations
    private final S3Uri htmlS3Uri;
    private final S3Uri xbrlS3Uri;
    
    // Presigned URLs for temporary download access (1-hour expiration)
    private final PresignedUrl htmlPresignedUrl;
    private final PresignedUrl xbrlPresignedUrl;
    
    // File sizes
    private final FileSize htmlFileSize;
    private final FileSize xbrlFileSize;
    
    // Quality metrics
    private final BigDecimal overallQualityScore;
    private final ComplianceStatus complianceStatus;
    
    // Performance metrics
    private final Duration generationDuration;
    
    private final Instant generatedAt;
    
    public ReportGeneratedEvent(
            String correlationId,
            ReportId reportId,
            BatchId batchId,
            BankId bankId,
            ReportType reportType,
            ReportingDate reportingDate,
            S3Uri htmlS3Uri,
            S3Uri xbrlS3Uri,
            PresignedUrl htmlPresignedUrl,
            PresignedUrl xbrlPresignedUrl,
            FileSize htmlFileSize,
            FileSize xbrlFileSize,
            BigDecimal overallQualityScore,
            ComplianceStatus complianceStatus,
            Duration generationDuration,
            Instant generatedAt) {
        super(correlationId, "ReportGenerated");
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.reportType = reportType;
        this.reportingDate = reportingDate;
        this.htmlS3Uri = htmlS3Uri;
        this.xbrlS3Uri = xbrlS3Uri;
        this.htmlPresignedUrl = htmlPresignedUrl;
        this.xbrlPresignedUrl = xbrlPresignedUrl;
        this.htmlFileSize = htmlFileSize;
        this.xbrlFileSize = xbrlFileSize;
        this.overallQualityScore = overallQualityScore;
        this.complianceStatus = complianceStatus;
        this.generationDuration = generationDuration;
        this.generatedAt = generatedAt;
    }
    
    @Override
    public String eventType() {
        return "ReportGenerated";
    }
}
