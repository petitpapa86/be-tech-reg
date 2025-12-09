/**
 * Report Generation Aggregate Package
 * 
 * <h2>Overview</h2>
 * This package contains the {@link com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport} 
 * aggregate root and its closely related value objects. According to Domain-Driven Design (DDD) principles, 
 * this package represents a cohesive unit of domain logic focused on report generation behavior.
 * 
 * <h2>Aggregate-Specific Value Objects</h2>
 * The value objects in this package are specific to report generation behavior and are tightly coupled 
 * to the GeneratedReport aggregate. They should NOT be used outside this aggregate context. These include:
 * 
 * <ul>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.generation.CalculatedExposure} - 
 *       Represents a single calculated large exposure with regulatory data specific to report generation</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.generation.ConcentrationIndices} - 
 *       Herfindahl-Hirschman indices for risk concentration assessment within generated reports</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.generation.GeographicBreakdown} - 
 *       Geographic distribution of exposures as calculated for report generation</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.generation.SectorBreakdown} - 
 *       Sector distribution of exposures as calculated for report generation</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.generation.ValidationResult} - 
 *       XBRL validation results specific to the report generation process</li>
 *   <li>{@link com.bcbs239.regtech.reportgeneration.domain.generation.ValidationError} - 
 *       Individual XBRL validation errors encountered during report generation</li>
 * </ul>
 * 
 * <h2>Design Rationale</h2>
 * <p>
 * These value objects are co-located with the GeneratedReport aggregate because they:
 * </p>
 * <ul>
 *   <li>Are used exclusively by the GeneratedReport aggregate and report generation logic</li>
 *   <li>Represent concepts specific to report generation behavior, not general domain concepts</li>
 *   <li>Form part of the aggregate's internal structure and behavior</li>
 *   <li>Should not be shared with other aggregates to maintain low coupling</li>
 * </ul>
 * 
 * <p>
 * This organization follows the DDD principle of <strong>high cohesion within aggregates</strong>. 
 * By keeping aggregate-specific value objects together with their aggregate, we make the aggregate's 
 * complete behavior immediately visible and maintainable.
 * </p>
 * 
 * <h2>Contrast with Shared Value Objects</h2>
 * <p>
 * Value objects that are used across multiple aggregates or layers (such as ReportId, BatchId, 
 * ReportStatus, etc.) are located in the {@code domain.shared.valueobjects} package. This separation 
 * ensures that:
 * </p>
 * <ul>
 *   <li>Aggregate-specific concepts remain encapsulated within their aggregate</li>
 *   <li>Cross-cutting domain concepts are available for reuse without creating aggregate dependencies</li>
 *   <li>The domain structure clearly communicates usage patterns and relationships</li>
 * </ul>
 * 
 * <h2>Guidelines for New Value Objects</h2>
 * <p>
 * When creating new value objects related to report generation:
 * </p>
 * <ul>
 *   <li>If the value object is used ONLY by GeneratedReport → place it in this package</li>
 *   <li>If the value object is used by multiple aggregates → place it in domain.shared.valueobjects</li>
 *   <li>When in doubt, start in this package and move to shared only when a second usage appears</li>
 * </ul>
 * 
 * <p>
 * For detailed guidelines on value object placement, see the VALUE_OBJECT_GUIDELINES.md document 
 * in the regtech-report-generation/domain/ directory.
 * </p>
 * 
 * @see com.bcbs239.regtech.reportgeneration.domain.generation.GeneratedReport
 * @see com.bcbs239.regtech.reportgeneration.domain.shared.valueobjects
 */
package com.bcbs239.regtech.reportgeneration.domain.generation;
