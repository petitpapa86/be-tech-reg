package com.bcbs239.regtech.dataquality.application.filemanagement.dto;

import com.bcbs239.regtech.dataquality.domain.validation.ViolationSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class FileResponse {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("filename")
    private String filename;
    
    @JsonProperty("uploadDate")
    private Instant uploadDate;

    @JsonProperty("uploadDateFormatted")
    private String uploadDateFormatted;

    @JsonProperty("uploadTimeFormatted")
    private String uploadTimeFormatted;
    
    @JsonProperty("size")
    private long size;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("bankId")
    private String bankId;
    
    @JsonProperty("batchId")
    private String batchId;

    @JsonProperty("qualityScore")
    private Double qualityScore;

    @JsonProperty("complianceScore")
    private Double complianceScore;

    @JsonProperty("violations")
    private ViolationSummary violations;

    // UI Metadata
    @JsonProperty("qualityScoreColor")
    private String qualityScoreColor;

    @JsonProperty("qualityScoreBadge")
    private String qualityScoreBadge;
    
    @JsonProperty("complianceScoreColor")
    private String complianceScoreColor;

    @JsonProperty("complianceBadge")
    private String complianceBadge;

    @JsonProperty("statusColor")
    private String statusColor;

    @JsonProperty("statusIcon")
    private String statusIcon;

    @JsonProperty("violationsColor")
    private String violationsColor;

    @JsonProperty("violationsSeverity")
    private String violationsSeverity;

    public FileResponse(
            String id, 
            String filename, 
            Instant uploadDate, 
            String uploadDateFormatted,
            String uploadTimeFormatted,
            long size, 
            String status, 
            String format, 
            String bankId, 
            String batchId,
            Double qualityScore,
            Double complianceScore,
            ViolationSummary violations,
            String qualityScoreColor,
            String qualityScoreBadge,
            String complianceScoreColor,
            String complianceBadge,
            String statusColor,
            String statusIcon,
            String violationsColor,
            String violationsSeverity) {
        this.id = id;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.uploadDateFormatted = uploadDateFormatted;
        this.uploadTimeFormatted = uploadTimeFormatted;
        this.size = size;
        this.status = status;
        this.format = format;
        this.bankId = bankId;
        this.batchId = batchId;
        this.qualityScore = qualityScore;
        this.complianceScore = complianceScore;
        this.violations = violations;
        this.qualityScoreColor = qualityScoreColor;
        this.qualityScoreBadge = qualityScoreBadge;
        this.complianceScoreColor = complianceScoreColor;
        this.complianceBadge = complianceBadge;
        this.statusColor = statusColor;
        this.statusIcon = statusIcon;
        this.violationsColor = violationsColor;
        this.violationsSeverity = violationsSeverity;
    }

    // Getters
    public String getId() { return id; }
    public String getFilename() { return filename; }
    public Instant getUploadDate() { return uploadDate; }
    public String getUploadDateFormatted() { return uploadDateFormatted; }
    public String getUploadTimeFormatted() { return uploadTimeFormatted; }
    public long getSize() { return size; }
    public String getStatus() { return status; }
    public String getFormat() { return format; }
    public String getBankId() { return bankId; }
    public String getBatchId() { return batchId; }
    public Double getQualityScore() { return qualityScore; }
    public Double getComplianceScore() { return complianceScore; }
    public ViolationSummary getViolations() { return violations; }
    public String getQualityScoreColor() { return qualityScoreColor; }
    public String getQualityScoreBadge() { return qualityScoreBadge; }
    public String getComplianceScoreColor() { return complianceScoreColor; }
    public String getComplianceBadge() { return complianceBadge; }
    public String getStatusColor() { return statusColor; }
    public String getStatusIcon() { return statusIcon; }
    public String getViolationsColor() { return violationsColor; }
    public String getViolationsSeverity() { return violationsSeverity; }
}
