package com.bcbs239.regtech.metrics.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "compliance_reports", schema = "metrics")
public class ComplianceReportEntity {

    @Id
    @Column(name = "report_id", nullable = false, length = 64)
    private String reportId;

    @Column(name = "batch_id", nullable = false, length = 128)
    private String batchId;

    @Column(name = "bank_id", nullable = false, length = 128)
    private String bankId;

    @Column(name = "report_type", nullable = false, length = 64)
    private String reportType;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "html_s3_uri")
    private String htmlS3Uri;

    @Column(name = "xbrl_s3_uri")
    private String xbrlS3Uri;

    @Column(name = "html_presigned_url")
    private String htmlPresignedUrl;

    @Column(name = "xbrl_presigned_url")
    private String xbrlPresignedUrl;

    @Column(name = "html_file_size")
    private Long htmlFileSize;

    @Column(name = "xbrl_file_size")
    private Long xbrlFileSize;

    @Column(name = "overall_quality_score", precision = 10, scale = 2)
    private BigDecimal overallQualityScore;

    @Column(name = "compliance_status", length = 64)
    private String complianceStatus;

    @Column(name = "generation_duration_millis")
    private Long generationDurationMillis;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ComplianceReportEntity() {
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
        this.bankId = bankId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public void setReportingDate(LocalDate reportingDate) {
        this.reportingDate = reportingDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getHtmlS3Uri() {
        return htmlS3Uri;
    }

    public void setHtmlS3Uri(String htmlS3Uri) {
        this.htmlS3Uri = htmlS3Uri;
    }

    public String getXbrlS3Uri() {
        return xbrlS3Uri;
    }

    public void setXbrlS3Uri(String xbrlS3Uri) {
        this.xbrlS3Uri = xbrlS3Uri;
    }

    public String getHtmlPresignedUrl() {
        return htmlPresignedUrl;
    }

    public void setHtmlPresignedUrl(String htmlPresignedUrl) {
        this.htmlPresignedUrl = htmlPresignedUrl;
    }

    public String getXbrlPresignedUrl() {
        return xbrlPresignedUrl;
    }

    public void setXbrlPresignedUrl(String xbrlPresignedUrl) {
        this.xbrlPresignedUrl = xbrlPresignedUrl;
    }

    public Long getHtmlFileSize() {
        return htmlFileSize;
    }

    public void setHtmlFileSize(Long htmlFileSize) {
        this.htmlFileSize = htmlFileSize;
    }

    public Long getXbrlFileSize() {
        return xbrlFileSize;
    }

    public void setXbrlFileSize(Long xbrlFileSize) {
        this.xbrlFileSize = xbrlFileSize;
    }

    public BigDecimal getOverallQualityScore() {
        return overallQualityScore;
    }

    public void setOverallQualityScore(BigDecimal overallQualityScore) {
        this.overallQualityScore = overallQualityScore;
    }

    public String getComplianceStatus() {
        return complianceStatus;
    }

    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }

    public Long getGenerationDurationMillis() {
        return generationDurationMillis;
    }

    public void setGenerationDurationMillis(Long generationDurationMillis) {
        this.generationDurationMillis = generationDurationMillis;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
