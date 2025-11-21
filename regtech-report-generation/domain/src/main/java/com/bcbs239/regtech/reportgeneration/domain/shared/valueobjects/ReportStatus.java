package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;

/**
 * Status of report generation process
 * Represents the lifecycle states of a generated report
 * 
 * <p><strong>Shared Value Object:</strong> This value object is used across multiple layers
 * including the domain layer, application layer, and presentation layer for queries and events.
 * It represents a cross-cutting domain concept that tracks the state of report generation
 * throughout the system. Following DDD's "shared kernel" pattern, it resides in the
 * {@code shared/valueobjects} package to enable consistent status representation across
 * all layers and contexts without creating tight coupling.</p>
 * 
 * <p>This enum is part of the shared kernel and is used in domain events, queries, and
 * API responses to communicate report generation state.</p>
 * 
 * @see com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects
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
