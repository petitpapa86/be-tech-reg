package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

/**
 * Status of report generation process
 * Represents the lifecycle states of a generated report
 */
public enum ReportStatus {
    /**
     * Report generation has been initiated but not yet started
     */
    PENDING,
    
    /**
     * Report generation is currently in progress
     */
    IN_PROGRESS,
    
    /**
     * Report generation completed successfully with both HTML and XBRL
     */
    COMPLETED,
    
    /**
     * Report generation partially completed (only one format generated)
     */
    PARTIAL,
    
    /**
     * Report generation failed completely
     */
    FAILED
}
