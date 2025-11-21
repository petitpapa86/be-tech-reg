package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.core.domain.shared.Entity;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGeneratedEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGenerationFailedEvent;
import com.bcbs239.regtech.reportgeneration.domain.generation.events.ReportGenerationStartedEvent;
import com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * GeneratedReport Aggregate Root
 * 
 * Represents a complete comprehensive report generation combining Large Exposures 
 * and Data Quality analysis with HTML and XBRL outputs.
 * 
 * Follows "tell, don't ask" principle - external code tells the aggregate what to do,
 * and the aggregate encapsulates all business logic and state transitions.
 * 
 * Key behaviors:
 * - Create new comprehensive report generation
 * - Record HTML report generation
 * - Record XBRL report generation
 * - Mark as completed (with validation)
 * - Mark as partial or failed
 * - Query completion status
 * 
 * Business rules:
 * - Report type is always COMPREHENSIVE (combines risk and quality)
 * - Report is COMPLETED only when both HTML and XBRL are generated
 * - Completion check is automatic when either report is recorded
 * - Domain events are raised on start, completion, or failure
 * - Failed or partial reports can be regenerated
 * - Quality score and compliance status are captured at generation time
 * 
 * Note: JPA annotations are applied in the infrastructure layer (GeneratedReportEntity)
 * to maintain clean domain model without infrastructure dependencies
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GeneratedReport extends Entity {
    
    private ReportId reportId;
    private BatchId batchId;
    private BankId bankId;
    private ReportingDate reportingDate;
    private ReportType reportType;
    private ReportStatus status;
    private HtmlReportMetadata htmlMetadata;
    private XbrlReportMetadata xbrlMetadata;
    private BigDecimal overallQualityScore;
    private ComplianceStatus complianceStatus;
    private ProcessingTimestamps timestamps;
    private FailureReason failureReason;
    
    /**
     * Factory method - Create comprehensive report
     * Creates a new comprehensive report generation combining Large Exposures and Data Quality analysis
     * 
     * Business rule: Report type is always COMPREHENSIVE
     * Raises ReportGenerationStartedEvent domain event
     * 
     * @param batchId The batch identifier
     * @param bankId The bank identifier
     * @param reportingDate The reporting date
     * @param qualityScore Overall quality score from quality validation
     * @param complianceStatus BCBS 239 compliance status
     * @return A new GeneratedReport in IN_PROGRESS status
     */
    public static GeneratedReport createComprehensiveReport(
            BatchId batchId,
            BankId bankId,
            ReportingDate reportingDate,
            BigDecimal qualityScore,
            ComplianceStatus complianceStatus) {
        
        ReportId reportId = ReportId.generate();
        
        GeneratedReport report = new GeneratedReport(
                reportId,
                batchId,
                bankId,
                reportingDate,
                ReportType.COMPREHENSIVE,
                ReportStatus.IN_PROGRESS,
                null, // htmlMetadata - will be set when HTML is generated
                null, // xbrlMetadata - will be set when XBRL is generated
                qualityScore,
                complianceStatus,
                ProcessingTimestamps.started(),
                null  // failureReason - only set on failure
        );
        
        // Register domain event
        report.addDomainEvent(new ReportGenerationStartedEvent(
                batchId.value(), // Use batchId as correlation ID
                reportId,
                batchId,
                bankId,
                Instant.now()
        ));
        
        return report;
    }
    
    /**
     * Mark HTML report as generated
     * Records HTML metadata and automatically checks if report is complete
     * 
     * Business rule: Cannot modify a completed report
     * 
     * @param htmlMetadata The HTML report metadata including S3 URI, file size, and presigned URL
     */
    public void markHtmlGenerated(HtmlReportMetadata htmlMetadata) {
        if (this.status == ReportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify a completed report");
        }
        
        this.htmlMetadata = htmlMetadata;
        this.timestamps = this.timestamps.withHtmlCompleted();
        
        // Automatically check if both reports are complete
        if (this.xbrlMetadata != null) {
            markCompleted();
        }
    }
    
    /**
     * Mark XBRL report as generated
     * Records XBRL metadata and automatically checks if report is complete
     * 
     * Business rule: Cannot modify a completed report
     * 
     * @param xbrlMetadata The XBRL report metadata including S3 URI, file size, presigned URL, and validation status
     */
    public void markXbrlGenerated(XbrlReportMetadata xbrlMetadata) {
        if (this.status == ReportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot modify a completed report");
        }
        
        this.xbrlMetadata = xbrlMetadata;
        this.timestamps = this.timestamps.withXbrlCompleted();
        
        // Automatically check if both reports are complete
        if (this.htmlMetadata != null) {
            markCompleted();
        }
    }
    
    /**
     * Mark report as completed
     * Private method called automatically when both HTML and XBRL are generated
     * 
     * Business rule: Can only complete if both HTML and XBRL reports exist
     * Raises ReportGeneratedEvent domain event
     */
    private void markCompleted() {
        if (this.htmlMetadata == null || this.xbrlMetadata == null) {
            throw new IllegalStateException("Cannot complete report without both HTML and XBRL");
        }
        
        this.status = ReportStatus.COMPLETED;
        this.timestamps = this.timestamps.withCompleted();
        
        // Raise domain event
        addDomainEvent(new ReportGeneratedEvent(
                this.batchId.value(), // Use batchId as correlation ID
                this.reportId,
                this.batchId,
                this.bankId,
                this.reportType,
                this.reportingDate,
                this.htmlMetadata.s3Uri(),
                this.xbrlMetadata.s3Uri(),
                this.htmlMetadata.presignedUrl(),
                this.xbrlMetadata.presignedUrl(),
                this.htmlMetadata.fileSize(),
                this.xbrlMetadata.fileSize(),
                this.overallQualityScore,
                this.complianceStatus,
                this.timestamps.getGenerationDuration(),
                Instant.now()
        ));
    }
    
    /**
     * Mark report as partial
     * Used when only one format was generated successfully
     * Still publishes ReportGeneratedEvent for partial reports
     * 
     * @param reason The reason for partial generation
     */
    public void markPartial(String reason) {
        this.status = ReportStatus.PARTIAL;
        this.failureReason = FailureReason.of(reason);
        this.timestamps = this.timestamps.withCompleted();
        
        // Still publish success event for partial reports (as per design)
        addDomainEvent(new ReportGeneratedEvent(
                this.batchId.value(),
                this.reportId,
                this.batchId,
                this.bankId,
                this.reportType,
                this.reportingDate,
                this.htmlMetadata != null ? this.htmlMetadata.s3Uri() : null,
                this.xbrlMetadata != null ? this.xbrlMetadata.s3Uri() : null,
                this.htmlMetadata != null ? this.htmlMetadata.presignedUrl() : null,
                this.xbrlMetadata != null ? this.xbrlMetadata.presignedUrl() : null,
                this.htmlMetadata != null ? this.htmlMetadata.fileSize() : null,
                this.xbrlMetadata != null ? this.xbrlMetadata.fileSize() : null,
                this.overallQualityScore,
                this.complianceStatus,
                this.timestamps.getGenerationDuration(),
                Instant.now()
        ));
    }
    
    /**
     * Mark report as failed
     * Raises ReportGenerationFailedEvent domain event
     * 
     * @param reason The failure reason
     */
    public void markFailed(FailureReason reason) {
        this.status = ReportStatus.FAILED;
        this.failureReason = reason;
        this.timestamps = this.timestamps.withFailed();
        
        // Raise domain event
        addDomainEvent(new ReportGenerationFailedEvent(
                this.batchId.value(), // Use batchId as correlation ID
                this.reportId,
                this.batchId,
                this.bankId,
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
     * Query method - Check if HTML report exists
     * 
     * @return true if HTML metadata is present
     */
    public boolean hasHtmlReport() {
        return htmlMetadata != null;
    }
    
    /**
     * Query method - Check if XBRL report exists
     * 
     * @return true if XBRL metadata is present
     */
    public boolean hasXbrlReport() {
        return xbrlMetadata != null;
    }
}
