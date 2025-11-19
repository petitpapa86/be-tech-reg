package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGeneratedEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGenerationFailedEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Optional;

/**
 * GeneratedReport Aggregate Root
 * 
 * Represents a complete report generation with HTML and XBRL outputs.
 * Follows "tell, don't ask" principle - external code tells the aggregate what to do,
 * and the aggregate encapsulates all business logic and state transitions.
 * 
 * Key behaviors:
 * - Create new report generation
 * - Record HTML report generation
 * - Record XBRL report generation
 * - Mark as completed (with validation)
 * - Mark as partial or failed
 * - Query completion status
 * 
 * Business rules:
 * - Report is COMPLETED only when both HTML and XBRL are generated
 * - Completion check is automatic when either report is recorded
 * - Domain events are raised on completion or failure
 * - Failed or partial reports can be regenerated
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class GeneratedReport extends Entity {
    
    private ReportId reportId;
    private BatchId batchId;
    private BankId bankId;
    private ReportingDate reportingDate;
    private ReportStatus status;
    private HtmlReportMetadata htmlMetadata;
    private XbrlReportMetadata xbrlMetadata;
    private ProcessingTimestamps timestamps;
    private Optional<FailureReason> failureReason;
    
    /**
     * Factory method - "Create yourself"
     * Creates a new report generation in PENDING status
     * 
     * @param batchId The batch identifier
     * @param bankId The bank identifier
     * @param reportingDate The reporting date
     * @return A new GeneratedReport in PENDING status
     */
    public static GeneratedReport create(BatchId batchId, BankId bankId, ReportingDate reportingDate) {
        return GeneratedReport.builder()
                .reportId(ReportId.generate())
                .batchId(batchId)
                .bankId(bankId)
                .reportingDate(reportingDate)
                .status(ReportStatus.PENDING)
                .timestamps(ProcessingTimestamps.start())
                .failureReason(Optional.empty())
                .build();
    }
    
    /**
     * Record HTML report generation
     * Automatically checks if report is complete after recording
     * 
     * @param s3Uri The S3 URI where HTML is stored
     * @param fileSize The size of the HTML file
     * @param presignedUrl The presigned URL for download
     */
    public void recordHtmlGeneration(S3Uri s3Uri, FileSize fileSize, PresignedUrl presignedUrl) {
        if (this.status == ReportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify a completed report");
        }
        
        this.htmlMetadata = new HtmlReportMetadata(s3Uri, fileSize, presignedUrl, Instant.now());
        this.status = ReportStatus.IN_PROGRESS;
        checkIfCompleted();
    }
    
    /**
     * Record XBRL report generation
     * Automatically checks if report is complete after recording
     * 
     * @param s3Uri The S3 URI where XBRL is stored
     * @param fileSize The size of the XBRL file
     * @param presignedUrl The presigned URL for download
     * @param validationStatus The XBRL validation status
     */
    public void recordXbrlGeneration(S3Uri s3Uri, FileSize fileSize, PresignedUrl presignedUrl, XbrlValidationStatus validationStatus) {
        if (this.status == ReportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify a completed report");
        }
        
        this.xbrlMetadata = new XbrlReportMetadata(s3Uri, fileSize, presignedUrl, validationStatus, Instant.now());
        this.status = ReportStatus.IN_PROGRESS;
        checkIfCompleted();
    }
    
    /**
     * Mark report as completed
     * Business rule: Can only complete if both HTML and XBRL reports exist
     * Raises ReportGeneratedEvent domain event
     */
    public void markAsCompleted() {
        if (!hasHtmlReport() || !hasXbrlReport()) {
            throw new IllegalStateException("Cannot complete report without both HTML and XBRL");
        }
        
        this.status = ReportStatus.COMPLETED;
        this.timestamps = this.timestamps.complete();
        
        // Raise domain event
        addDomainEvent(new ReportGeneratedEvent(
                this.batchId.value(), // Use batchId as correlation ID
                this.reportId,
                this.batchId,
                this.bankId,
                this.reportingDate,
                this.htmlMetadata.presignedUrl(),
                this.xbrlMetadata.presignedUrl(),
                Instant.now()
        ));
    }
    
    /**
     * Mark report as partial
     * Used when only one format was generated successfully
     * 
     * @param reason The reason for partial generation
     */
    public void markAsPartial(String reason) {
        this.status = ReportStatus.PARTIAL;
        this.failureReason = Optional.of(FailureReason.of(FailureReason.FailureCategory.UNKNOWN, reason));
        this.timestamps = this.timestamps.complete();
    }
    
    /**
     * Mark report as failed
     * Raises ReportGenerationFailedEvent domain event
     * 
     * @param reason The failure reason
     */
    public void markAsFailed(FailureReason reason) {
        this.status = ReportStatus.FAILED;
        this.failureReason = Optional.of(reason);
        this.timestamps = this.timestamps.complete();
        
        // Raise domain event
        addDomainEvent(new ReportGenerationFailedEvent(
                this.batchId.value(), // Use batchId as correlation ID
                this.batchId,
                reason,
                Instant.now()
        ));
    }
    
    /**
     * Query method - Check if report is completed
     * 
     * @return true if status is COMPLETED
     */
    public boolean isCompleted() {
        return status == ReportStatus.COMPLETED;
    }
    
    /**
     * Query method - Check if report can be regenerated
     * Failed or partial reports can be regenerated
     * 
     * @return true if status is FAILED or PARTIAL
     */
    public boolean canRegenerate() {
        return status == ReportStatus.FAILED || status == ReportStatus.PARTIAL;
    }
    
    /**
     * Private helper - Check if both reports are complete
     * Automatically marks as completed if both HTML and XBRL exist
     * Encapsulates business logic within aggregate
     */
    private void checkIfCompleted() {
        if (hasHtmlReport() && hasXbrlReport()) {
            markAsCompleted();
        }
    }
    
    /**
     * Private helper - Check if HTML report exists
     */
    private boolean hasHtmlReport() {
        return htmlMetadata != null;
    }
    
    /**
     * Private helper - Check if XBRL report exists
     */
    private boolean hasXbrlReport() {
        return xbrlMetadata != null;
    }
}
