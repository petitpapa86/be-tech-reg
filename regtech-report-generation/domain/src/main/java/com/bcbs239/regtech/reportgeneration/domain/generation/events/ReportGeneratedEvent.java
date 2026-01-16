package com.bcbs239.regtech.reportgeneration.domain.generation.events;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
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
    
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ReportGeneratedEvent(
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("reportId") ReportId reportId,
            @JsonProperty("batchId") BatchId batchId,
            @JsonProperty("bankId") BankId bankId,
            @JsonProperty("reportType") ReportType reportType,
            @JsonProperty("reportingDate") ReportingDate reportingDate,
            @JsonProperty("htmlS3Uri") S3Uri htmlS3Uri,
            @JsonProperty("xbrlS3Uri") S3Uri xbrlS3Uri,
            @JsonProperty("htmlPresignedUrl") PresignedUrl htmlPresignedUrl,
            @JsonProperty("xbrlPresignedUrl") PresignedUrl xbrlPresignedUrl,
            @JsonProperty("htmlFileSize") FileSize htmlFileSize,
            @JsonProperty("xbrlFileSize") FileSize xbrlFileSize,
            @JsonProperty("overallQualityScore") BigDecimal overallQualityScore,
            @JsonProperty("complianceStatus") ComplianceStatus complianceStatus,
            @JsonProperty("generationDuration") Duration generationDuration,
            @JsonProperty("generatedAt") Instant generatedAt) {
        super(correlationId);
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

}
