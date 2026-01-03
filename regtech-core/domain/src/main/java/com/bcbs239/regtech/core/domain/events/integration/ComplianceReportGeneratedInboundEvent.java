package com.bcbs239.regtech.core.domain.events.integration;

import com.bcbs239.regtech.core.domain.events.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class ComplianceReportGeneratedInboundEvent extends DomainEvent {

    private static final String EVENT_VERSION = "1.0";

    private final String reportId;
    private final String batchId;
    private final String bankId;
    private final String reportType;
    private final String reportingDate;

    private final String htmlS3Uri;
    private final String xbrlS3Uri;

    private final String htmlPresignedUrl;
    private final String xbrlPresignedUrl;

    private final Long htmlFileSize;
    private final Long xbrlFileSize;

    private final BigDecimal overallQualityScore;
    private final String complianceStatus;

    private final Long generationDurationMillis;

    private final Instant generatedAt;
    private final String eventVersion;

    @JsonCreator
    public ComplianceReportGeneratedInboundEvent(
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
            @JsonProperty("correlationId") String correlationId
    ) {
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
}
