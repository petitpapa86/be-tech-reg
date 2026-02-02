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
    private final ReportStatus status;
    private final Instant generatedAt;
    private final Instant updatedAt;

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
            Instant updatedAt,
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
        this.status = ReportStatus.fromString(status);
        this.generatedAt = generatedAt;
        this.updatedAt = updatedAt;
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

    public String getStatus() {
        return status != null ? status.name() : null;
    }

    public ReportStatus getStatusEnum() {
        return status;
    }

    public ComplianceReport updateStatus(ReportStatus newStatus) {
        return new ComplianceReport(
                this.reportId,
                this.batchId,
                this.bankId,
                this.reportingDate,
                this.reportType,
                newStatus.name(),
                this.generatedAt,
                Instant.now(), // updatedAt
                this.htmlS3Uri,
                this.xbrlS3Uri,
                this.htmlPresignedUrl,
                this.xbrlPresignedUrl,
                this.htmlFileSize,
                this.xbrlFileSize,
                this.overallQualityScore,
                this.complianceStatus,
                this.generationDurationMillis
        );
    }

}
