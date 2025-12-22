package com.bcbs239.regtech.reportgeneration.application.integration.events;

import com.bcbs239.regtech.core.domain.events.IntegrationEvent;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Integration event published when a comprehensive report is successfully generated.
 * This event notifies downstream modules (notification service, audit service)
 * that a report is available for download.
 * 
 * Requirements: 14.1, 15.1, 15.2
 * Event versioning: v1.0 - Initial version with comprehensive report metadata
 */
@Getter
public class ReportGeneratedIntegrationEvent extends IntegrationEvent {
    
    private static final String EVENT_VERSION = "1.0";
    
    private final String reportId;
    private final String batchId;
    private final String bankId;
    private final String reportType;
    private final String reportingDate;
    
    // S3 URIs for file locations
    private final String htmlS3Uri;
    private final String xbrlS3Uri;
    
    // Presigned URLs for temporary download access (1-hour expiration)
    private final String htmlPresignedUrl;
    private final String xbrlPresignedUrl;
    
    // File sizes in bytes
    private final Long htmlFileSize;
    private final Long xbrlFileSize;
    
    // Quality metrics
    private final BigDecimal overallQualityScore;
    private final String complianceStatus;
    
    // Performance metrics
    private final Long generationDurationMillis;
    
    private final Instant generatedAt;
    private final String eventVersion;
    
    @JsonCreator
    public ReportGeneratedIntegrationEvent(
            @JsonProperty("reportId") String reportId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("bankId") String bankId,
            @JsonProperty("reportType") String reportType,
            @JsonProperty("reportingDate") String reportingDate,
            @JsonProperty("htmlS3Uri") String htmlS3Uri,
            @JsonProperty("xbrlS3Uri") String xbrlS3Uri,
            @JsonProperty("htmlPresignedUrl") String htmlPresignedUrl,
            @JsonProperty("xbrlPresignedUrl") String xbrlPresignedUrl,
            @JsonProperty("htmlFileSize") Long htmlFileSize,
            @JsonProperty("xbrlFileSize") Long xbrlFileSize,
            @JsonProperty("overallQualityScore") BigDecimal overallQualityScore,
            @JsonProperty("complianceStatus") String complianceStatus,
            @JsonProperty("generationDurationMillis") Long generationDurationMillis,
            @JsonProperty("generatedAt") Instant generatedAt,
            @JsonProperty("correlationId") String correlationId) {
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
        this.generationDurationMillis = generationDurationMillis;
        this.generatedAt = generatedAt;
        this.eventVersion = EVENT_VERSION;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ReportGeneratedIntegrationEvent{reportId='%s', batchId='%s', bankId='%s', reportType='%s', " +
            "reportingDate='%s', htmlS3Uri='%s', xbrlS3Uri='%s', overallQualityScore=%s, complianceStatus='%s', " +
            "generationDurationMillis=%d, generatedAt=%s, version='%s'}",
            reportId, batchId, bankId, reportType, reportingDate, htmlS3Uri, xbrlS3Uri, 
            overallQualityScore, complianceStatus, generationDurationMillis, generatedAt, eventVersion
        );
    }
}
