/**
 * Shared Value Objects - Cross-Cutting Domain Concepts
 * 
 * <h2>Purpose</h2>
 * This package contains value objects that are used across multiple aggregates, layers, or contexts
 * within the report generation bounded context. These represent the "shared kernel" in Domain-Driven
 * Design (DDD) - domain concepts that are fundamental and reused throughout the system.
 * 
 * <h2>Shared Kernel Concept</h2>
 * In DDD, a shared kernel is a subset of the domain model that is shared between multiple bounded contexts
 * or aggregates. These are carefully selected concepts that:
 * <ul>
 *   <li>Represent fundamental domain concepts used across multiple contexts</li>
 *   <li>Have consistent meaning and behavior wherever they appear</li>
 *   <li>Reduce duplication while maintaining clear boundaries</li>
 *   <li>Enable loose coupling between aggregates</li>
 * </ul>
 * 
 * <h2>Value Objects by Category</h2>
 * 
 * <h3>Identity Value Objects</h3>
 * Value objects that uniquely identify domain entities across contexts:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportId} - 
 *       Unique identifier for generated reports, used across domain, application, and infrastructure layers</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchId} - 
 *       Identifier for data processing batches, shared with ingestion and quality modules</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BankId} - 
 *       Identifier for banking institutions, used across all regulatory modules</li>
 * </ul>
 * 
 * <h3>Temporal Value Objects</h3>
 * Value objects representing time-related concepts:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportingDate} - 
 *       The regulatory reporting date, used in queries, events, and report metadata</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ProcessingTimestamps} - 
 *       Tracks processing lifecycle timestamps, used across multiple aggregates for audit trails</li>
 * </ul>
 * 
 * <h3>Status Value Objects</h3>
 * Enumerations and status indicators used in queries and events:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportStatus} - 
 *       Current state of report generation (PENDING, COMPLETED, FAILED), used in queries and events</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ComplianceStatus} - 
 *       Regulatory compliance status, shared across quality and report generation contexts</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.XbrlValidationStatus} - 
 *       XBRL validation outcome status, used in report metadata and quality checks</li>
 * </ul>
 * 
 * <h3>Metadata Value Objects</h3>
 * Value objects encapsulating report metadata:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.HtmlReportMetadata} - 
 *       Metadata for HTML reports, used in aggregate and application layer</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.XbrlReportMetadata} - 
 *       Metadata for XBRL reports, used in aggregate and application layer</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.BatchContext} - 
 *       Contextual information about batch processing, shared across modules</li>
 * </ul>
 * 
 * <h3>Infrastructure-Related Value Objects</h3>
 * Value objects that bridge domain and infrastructure concerns:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.S3Uri} - 
 *       S3 storage location, used across domain and infrastructure layers</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.PresignedUrl} - 
 *       Temporary access URL for reports, used in application and presentation layers</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FileSize} - 
 *       File size with validation, used in storage and metadata contexts</li>
 * </ul>
 * 
 * <h3>Quality Value Objects</h3>
 * Value objects related to data quality assessment:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.QualityGrade} - 
 *       Quality assessment grade, shared between quality and report generation modules</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.QualityDimension} - 
 *       Specific quality dimension being assessed, used across quality contexts</li>
 * </ul>
 * 
 * <h3>Other Shared Value Objects</h3>
 * Additional cross-cutting domain concepts:
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.AmountEur} - 
 *       Monetary amount in EUR with validation, used across calculation and reporting</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportType} - 
 *       Type of report being generated (HTML, XBRL, COMPREHENSIVE)</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.ReportContent} - 
 *       The actual content of a generated report</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects.FailureReason} - 
 *       Reason for processing failures, used in error handling across contexts</li>
 * </ul>
 * 
 * <h2>Placement Guidelines</h2>
 * Value objects belong in this shared package when they:
 * <ul>
 *   <li>Are used by multiple aggregates within the bounded context</li>
 *   <li>Are referenced across different layers (domain, application, infrastructure)</li>
 *   <li>Appear in integration events or cross-module communication</li>
 *   <li>Represent fundamental domain concepts with consistent meaning everywhere</li>
 * </ul>
 * 
 * <h2>Contrast with Aggregate-Specific Value Objects</h2>
 * Value objects that are tightly coupled to a single aggregate's behavior should NOT be placed here.
 * For example, {@code CalculatedExposure} and {@code ConcentrationIndices} are specific to the
 * {@code GeneratedReport} aggregate and belong in the {@code generation} package.
 * 
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> All value objects are immutable</li>
 *   <li><strong>Value Equality:</strong> Equality based on attributes, not identity</li>
 *   <li><strong>Self-Validation:</strong> Value objects validate their own invariants</li>
 *   <li><strong>Ubiquitous Language:</strong> Names reflect domain terminology</li>
 *   <li><strong>Minimal Dependencies:</strong> No dependencies on infrastructure or external libraries</li>
 * </ul>
 * 
 * @see com.bcbs239.regtech.reportgeneration.domain.generation For aggregate-specific value objects
 */
package com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects;
