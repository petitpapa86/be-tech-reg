package com.bcbs239.regtech.metrics.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Persistence entity for dashboard metrics.
 * Single-row table (id = 1) holding aggregate dashboard stats.
 */
@Entity
@Table(name = "dashboard_metrics", schema = "metrics")
public class DashboardMetricsEntity {

    @EmbeddedId
    private DashboardMetricsKey key;

    @Column(name = "overall_score")
    private Double overallScore = 0.0;

    @Column(name = "data_quality_score")
    private Double dataQualityScore = 0.0;

    @Column(name = "bcbs_rules_score")
    private Double bcbsRulesScore = 0.0;

    @Column(name = "completeness_score")
    private Double completenessScore = 0.0;

    // Serialized TDigest sketches for medians (infrastructure-only)
    @Column(name = "data_quality_digest", columnDefinition = "bytea")
    private byte[] dataQualityDigest;

    @Column(name = "completeness_digest", columnDefinition = "bytea")
    private byte[] completenessDigest;

    @Column(name = "total_files_processed")
    private Integer totalFilesProcessed = 0;

    @Column(name = "total_violations")
    private Integer totalViolations = 0;

    @Column(name = "total_reports_generated")
    private Integer totalReportsGenerated = 0;

    @Column(name = "total_exposures")
    private Integer totalExposures = 0;

    @Column(name = "valid_exposures")
    private Integer validExposures = 0;

    @Column(name = "total_errors")
    private Integer totalErrors = 0;

    @Version
    private Long version;

    public DashboardMetricsEntity() {
    }

    public DashboardMetricsKey getKey() {
        return key;
    }

    public void setKey(DashboardMetricsKey key) {
        this.key = key;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getDataQualityScore() {
        return dataQualityScore;
    }

    public void setDataQualityScore(Double dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }

    public Double getBcbsRulesScore() {
        return bcbsRulesScore;
    }

    public void setBcbsRulesScore(Double bcbsRulesScore) {
        this.bcbsRulesScore = bcbsRulesScore;
    }

    public Double getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(Double completenessScore) {
        this.completenessScore = completenessScore;
    }

    public byte[] getDataQualityDigest() {
        return dataQualityDigest;
    }

    public void setDataQualityDigest(byte[] dataQualityDigest) {
        this.dataQualityDigest = dataQualityDigest;
    }

    public byte[] getCompletenessDigest() {
        return completenessDigest;
    }

    public void setCompletenessDigest(byte[] completenessDigest) {
        this.completenessDigest = completenessDigest;
    }

    public Integer getTotalFilesProcessed() {
        return totalFilesProcessed;
    }

    public void setTotalFilesProcessed(Integer totalFilesProcessed) {
        this.totalFilesProcessed = totalFilesProcessed;
    }

    public Integer getTotalViolations() {
        return totalViolations;
    }

    public void setTotalViolations(Integer totalViolations) {
        this.totalViolations = totalViolations;
    }

    public Integer getTotalReportsGenerated() {
        return totalReportsGenerated;
    }

    public void setTotalReportsGenerated(Integer totalReportsGenerated) {
        this.totalReportsGenerated = totalReportsGenerated;
    }

    public Integer getTotalExposures() {
        return totalExposures;
    }

    public void setTotalExposures(Integer totalExposures) {
        this.totalExposures = totalExposures;
    }

    public Integer getValidExposures() {
        return validExposures;
    }

    public void setValidExposures(Integer validExposures) {
        this.validExposures = validExposures;
    }

    public Integer getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(Integer totalErrors) {
        this.totalErrors = totalErrors;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
