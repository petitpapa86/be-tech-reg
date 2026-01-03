package com.bcbs239.regtech.metrics.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
@Getter
public class ComplianceReport {

    private final String reportId;
    private final String batchId;
    private final BankId bankId;
    private final LocalDate reportingDate;
    private final String reportType;
    private final String status;
    private final Instant generatedAt;

    private final String htmlS3Uri;
    private final String xbrlS3Uri;
    private final String htmlPresignedUrl;
    private final String xbrlPresignedUrl;
    private final Long htmlFileSize;
    private final Long xbrlFileSize;

    private final BigDecimal overallQualityScore;
    private final String complianceStatus;
    private final Long generationDurationMillis;

    public ComplianceReport(
            String reportId,
            String batchId,
            BankId bankId,
            LocalDate reportingDate,
            String reportType,
            String status,
            Instant generatedAt,
            String htmlS3Uri,
            String xbrlS3Uri,
            String htmlPresignedUrl,
            String xbrlPresignedUrl,
            Long htmlFileSize,
            Long xbrlFileSize,
            BigDecimal overallQualityScore,
            String complianceStatus,
            Long generationDurationMillis
    ) {
        this.reportId = reportId;
        this.batchId = batchId;
        this.bankId = bankId;
        this.reportingDate = reportingDate;
        this.reportType = reportType;
        this.status = status;
        this.generatedAt = generatedAt;
        this.htmlS3Uri = htmlS3Uri;
        this.xbrlS3Uri = xbrlS3Uri;
        this.htmlPresignedUrl = htmlPresignedUrl;
        this.xbrlPresignedUrl = xbrlPresignedUrl;
        this.htmlFileSize = htmlFileSize;
        this.xbrlFileSize = xbrlFileSize;
        this.overallQualityScore = overallQualityScore;
        this.complianceStatus = complianceStatus;
        this.generationDurationMillis = generationDurationMillis;
    }

}
