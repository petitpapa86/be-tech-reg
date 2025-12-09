# Requirements Document

## Introduction

The Report Generation Module generates comprehensive HTML and XBRL-XML reports combining Large Exposures (Grandi Esposizioni) analysis with Data Quality validation results. The module is compliant with CRR Regulation (EU) No. 575/2013 and EBA ITS standards. The module is triggered by dual Spring Application Events from the Risk Calculation Module and Data Quality Module, coordinates their arrival, retrieves calculation and quality results from S3 storage (production) or local filesystem (development), generates professional reports with dynamic quality recommendations, and persists metadata for audit and retrieval purposes.

## Glossary

- **Report_Generation_Module**: The bounded context responsible for generating comprehensive reports combining Large Exposures and Data Quality analysis in HTML and XBRL formats
- **Risk_Calculation_Module**: Upstream bounded context that calculates risk metrics and publishes BatchCalculationCompletedEvent with S3 URI to calculation results JSON
- **Data_Quality_Module**: Upstream bounded context that validates data quality and publishes BatchQualityCompletedEvent with S3 URI to quality results JSON
- **BatchCalculationCompletedEvent**: Integration event indicating risk calculation completion for a batch, contains S3 URI to calc_batch_{id}.json
- **BatchQualityCompletedEvent**: Integration event indicating quality validation completion for a batch, contains S3 URI to quality_batch_{id}.json
- **Generated_Report**: Aggregate root representing a complete comprehensive report generation with HTML and XBRL outputs
- **Report_Coordinator**: Domain service that coordinates arrival of dual events before triggering generation
- **Comprehensive_Report**: Combined report containing both Large Exposures analysis and Data Quality validation sections
- **Large_Exposures**: Credit exposures equal to or exceeding 10% of eligible capital as defined in CRR Article 392
- **Data_Quality_Report**: Section of comprehensive report showing quality scores, dimension analysis, error distribution, and dynamic recommendations
- **Quality_Recommendations_Generator**: Domain service that generates contextual recommendations based on specific quality issues found
- **CRR**: Capital Requirements Regulation (EU) No. 575/2013
- **EBA**: European Banking Authority
- **XBRL**: eXtensible Business Reporting Language for regulatory reporting
- **ITS**: Implementing Technical Standards (Commission Implementing Regulation EU No 680/2014)
- **COREP**: Common Reporting framework for supervisory reporting
- **BCBS_239**: Basel Committee on Banking Supervision Principles for effective risk data aggregation and risk reporting
- **File_Storage_Service**: Infrastructure service abstraction for S3 and local filesystem operations
- **LEI**: Legal Entity Identifier (20-character alphanumeric code)
- **ESA_2010**: European System of Accounts 2010 for sector classification
- **Presigned_URL**: Temporary authenticated URL for secure file access without AWS credentials
- **Event_Retry_Processor**: Core infrastructure component for automatic retry of failed event processing
- **Thymeleaf**: Server-side Java template engine for HTML generation
- **Chart_js**: JavaScript library for interactive data visualization
- **Quality_Dimension**: Aspect of data quality (Completeness, Accuracy, Consistency, Timeliness, Uniqueness, Validity)

## Requirements

### Requirement 1

**User Story:** As a risk analyst, I want the system to automatically generate reports when both risk calculation and quality validation complete, so that I receive comprehensive analysis without manual intervention.

#### Acceptance Criteria

1. WHEN BatchCalculationCompletedEvent arrives from Risk Calculation Module, THE Report Generation Module SHALL validate the event using event listener pattern
2. WHEN BatchQualityCompletedEvent arrives from Data Quality Module, THE Report Generation Module SHALL validate the event using event listener pattern
3. WHEN either event validation succeeds, THE Report Generation Module SHALL check idempotency to prevent duplicate processing
4. WHEN idempotency check passes, THE Report Generation Module SHALL register event with Report Coordinator and check if both events are present
5. WHEN BOTH events are present for a batch, THE Report Generation Module SHALL trigger asynchronous comprehensive report generation using @Async annotation
6. WHEN event is invalid or stale (older than 24 hours), THE Report Generation Module SHALL log warning and skip processing
7. WHEN event processing fails, THE Report Generation Module SHALL save EventProcessingFailure record for automatic retry by EventRetryProcessor

### Requirement 2

**User Story:** As a system administrator, I want event processing to be reliable and thread-safe, so that the system handles concurrent events correctly.

#### Acceptance Criteria

1. WHEN multiple events arrive concurrently for different batches, THE Report Generation Module SHALL process each batch independently using dedicated thread pool
2. WHEN event listener receives an event, THE Report Generation Module SHALL use @Async annotation with named executor for async processing
3. WHEN checking for duplicate processing, THE Report Generation Module SHALL query generated_reports table by batch_id for idempotency
4. WHEN event processing encounters errors, THE Report Generation Module SHALL use EventProcessingFailure repository to persist failure details
5. WHEN EventRetryProcessor retries failed events, THE Report Generation Module SHALL reprocess using the same event listener handler

### Requirement 3

**User Story:** As a compliance officer, I want the system to retrieve calculation and quality results from secure storage, so that reports are based on validated and auditable data sources.

#### Acceptance Criteria

1. WHEN running in production mode, THE Report Generation Module SHALL download calculation results from S3 URI provided in BatchCalculationCompletedEvent (s3://risk-analysis-production/calculated/calc_batch_{id}.json)
2. WHEN running in production mode, THE Report Generation Module SHALL download quality results from S3 URI provided in BatchQualityCompletedEvent (s3://risk-analysis-production/quality/quality_batch_{id}.json)
3. WHEN running in development mode, THE Report Generation Module SHALL read calculation files from local filesystem /data/calculated/calc_batch_{id}.json
4. WHEN running in development mode, THE Report Generation Module SHALL read quality files from local filesystem /data/quality/quality_batch_{id}.json
5. WHEN downloading files from S3, THE Report Generation Module SHALL use IFileStorageService from regtech-core-infrastructure
6. WHEN file download fails, THE Report Generation Module SHALL use EventRetryProcessor from regtech-core-application for retry with exponential backoff
7. WHEN file storage operations are needed, THE Report Generation Module SHALL leverage shared S3FileStorageService and LocalFileStorageService from regtech-core-infrastructure

### Requirement 4

**User Story:** As a data quality manager, I want the system to validate retrieved data integrity, so that reports are only generated from complete and valid data.

#### Acceptance Criteria

1. WHEN JSON parsing fails for either calculation or quality data, THE Report Generation Module SHALL log an error with parse exception details, save malformed JSON for analysis, and not generate a report
2. WHEN required fields are missing from retrieved data, THE Report Generation Module SHALL log an error with specific validation failures and not generate a report
3. WHEN checksum validation fails, THE Report Generation Module SHALL log an error and not generate a report
4. WHEN data validation succeeds for both files, THE Report Generation Module SHALL map JSON to Java DTOs and proceed with report generation
5. WHEN file download exceeds 30 seconds, THE Report Generation Module SHALL timeout and log an error

### Requirement 5

**User Story:** As a risk manager, I want a comprehensive HTML report combining Large Exposures and Data Quality analysis, so that I can assess both risk exposure and data reliability in a single view.

#### Acceptance Criteria

1. WHEN generating comprehensive report, THE Report Generation Module SHALL wait for BOTH BatchCalculationCompletedEvent AND BatchQualityCompletedEvent before starting generation
2. WHEN both events are received, THE Report Generation Module SHALL fetch BOTH calculation JSON and quality JSON files from storage
3. WHEN rendering HTML report, THE Report Generation Module SHALL include Large Exposures section with risk analysis and Data Quality section with quality metrics
4. WHEN displaying report header, THE Report Generation Module SHALL show overall quality score badge and compliance status alongside bank information
5. WHEN completing report generation, THE Report Generation Module SHALL generate single HTML file containing both sections and XBRL file for regulatory submission

### Requirement 6

**User Story:** As a risk analyst, I want a professional HTML report with interactive charts for Large Exposures, so that I can analyze exposure data effectively for internal decision-making.

#### Acceptance Criteria

1. WHEN generating HTML report, THE Report Generation Module SHALL use Thymeleaf template engine with Tailwind CSS and Chart.js
2. WHEN rendering the Large Exposures section, THE Report Generation Module SHALL include header with bank information, reporting date, and regulatory references
3. WHEN displaying summary data, THE Report Generation Module SHALL show five executive summary cards for Tier 1 Capital, Large Exposures Count, Total Amount, Limit Breaches, and Sector Concentration
4. WHEN visualizing data, THE Report Generation Module SHALL generate a donut chart for sector distribution and horizontal bar chart for top exposures
5. WHEN presenting detailed data, THE Report Generation Module SHALL include a sortable table with all large exposures showing counterparty, LEI, sector, rating, amount, percentage of capital, and compliance status

### Requirement 7

**User Story:** As a compliance officer, I want the HTML report to include regulatory compliance information, so that I can assess adherence to CRR requirements.

#### Acceptance Criteria

1. WHEN displaying compliance status, THE Report Generation Module SHALL show counts of compliant exposures (≤25% of capital) and non-compliant exposures (>25% of capital)
2. WHEN rendering exposure details, THE Report Generation Module SHALL mark exposures exceeding 25% limit with red warning badges
3. WHEN including regulatory framework section, THE Report Generation Module SHALL reference CRR Articles 392 and 395, and ITS 680/2014
4. WHEN generating risk analysis section, THE Report Generation Module SHALL identify concentration risks and provide recommendations
5. WHEN completing the report, THE Report Generation Module SHALL include footer with generation timestamp and confidentiality notice

### Requirement 8

**User Story:** As a data quality manager, I want the HTML report to include comprehensive quality analysis with dynamic recommendations, so that I can understand specific data issues and take corrective actions.

#### Acceptance Criteria

1. WHEN displaying quality section, THE Report Generation Module SHALL show overall quality score with percentage, grade (A-F), and color-coded background
2. WHEN rendering dimension scores, THE Report Generation Module SHALL display scores for all six dimensions (Completeness, Accuracy, Consistency, Timeliness, Uniqueness, Validity) with progress bars
3. WHEN showing error distribution, THE Report Generation Module SHALL identify top 3 dimensions with most errors and display counts and percentages
4. WHEN generating recommendations, THE Report Generation Module SHALL analyze actual quality issues and generate contextual recommendation sections based on error patterns
5. WHEN displaying BCBS 239 compliance, THE Report Generation Module SHALL show compliance status for Principles 3, 4, 5, and 6 with color-coded badges

### Requirement 9

**User Story:** As a data quality manager, I want dynamic recommendations based on actual errors found, so that I receive specific actionable guidance rather than generic advice.

#### Acceptance Criteria

1. WHEN overall quality score is below 60%, THE Report Generation Module SHALL generate Critical Situation recommendation section with immediate action items
2. WHEN specific dimension score is below threshold (Completeness <70%, Accuracy <70%, Consistency <80%, Timeliness <90%, Uniqueness <95%, Validity <90%), THE Report Generation Module SHALL generate dimension-specific recommendation section
3. WHEN analyzing error patterns, THE Report Generation Module SHALL identify top 3 most common error types and generate error-specific recommendations with field names and counts
4. WHEN any dimension score is 95% or higher, THE Report Generation Module SHALL generate Positive Aspects section highlighting excellent dimensions
5. WHEN generating recommendations, THE Report Generation Module SHALL always include Action Plan section with short-term (1-2 weeks), medium-term (1-3 months), and long-term (3-6 months) actions

### Requirement 10

**User Story:** As a regulatory reporting officer, I want a valid XBRL-XML file conforming to EBA taxonomy, so that I can submit Large Exposures data to supervisory authorities.

#### Acceptance Criteria

1. WHEN generating XBRL file, THE Report Generation Module SHALL create XML conforming to EBA COREP Framework with templates LE1 and LE2
2. WHEN constructing XBRL structure, THE Report Generation Module SHALL include all required namespaces and schema reference to EBA taxonomy
3. WHEN adding exposure contexts, THE Report Generation Module SHALL create one context per large exposure with dimensions for counterparty (CP), country (CT), and sector (SC)
4. WHEN populating LE1 facts, THE Report Generation Module SHALL include counterparty name, LEI code, identifier type, country code, and sector code for each exposure
5. WHEN populating LE2 facts, THE Report Generation Module SHALL include original exposure amount, exposure after CRM, trading book portion, non-trading book portion, and percentage of eligible capital

### Requirement 11

**User Story:** As a data quality manager, I want XBRL validation against EBA schema, so that generated files meet regulatory technical requirements before submission.

#### Acceptance Criteria

1. WHEN XBRL generation completes, THE Report Generation Module SHALL validate the XML document against EBA XSD schema
2. WHEN validation detects errors, THE Report Generation Module SHALL log detailed validation errors with line numbers and error messages
3. WHEN validation fails, THE Report Generation Module SHALL save the invalid XML to filesystem for debugging and mark report status as PARTIAL
4. WHEN LEI code is missing for a counterparty, THE Report Generation Module SHALL use identifier type "CONCAT" with alternate identifier
5. WHEN validation succeeds, THE Report Generation Module SHALL format XML with pretty-print indentation and UTF-8 encoding

### Requirement 12

**User Story:** As a system administrator, I want generated reports stored securely in S3 with versioning, so that reports are durable, auditable, and accessible.

#### Acceptance Criteria

1. WHEN uploading reports to S3, THE Report Generation Module SHALL upload HTML to s3://risk-analysis-production/reports/html/ and XBRL to s3://risk-analysis-production/reports/xbrl/
2. WHEN constructing file names, THE Report Generation Module SHALL use pattern Comprehensive_Risk_Analysis_{BankID}_{YYYY-MM-DD}.{extension}
3. WHEN uploading files, THE Report Generation Module SHALL set Content-Type appropriately, enable AES-256 server-side encryption, and include metadata tags
4. WHEN uploads complete, THE Report Generation Module SHALL generate presigned URLs with 1-hour expiration for temporary download access
5. WHEN S3 upload fails after 3 retry attempts, THE Report Generation Module SHALL save files to local filesystem /tmp/deferred-reports/ and schedule retry

### Requirement 13

**User Story:** As an auditor, I want report metadata persisted in the database, so that I can track report generation history and access reports for compliance audits.

#### Acceptance Criteria

1. WHEN report generation completes successfully, THE Report Generation Module SHALL insert a record in generated_reports table with status COMPLETED and report_type COMPREHENSIVE
2. WHEN only one file generates successfully, THE Report Generation Module SHALL insert a record with status PARTIAL
3. WHEN report generation fails completely, THE Report Generation Module SHALL insert a record with status FAILED and failure reason
4. WHEN saving metadata, THE Report Generation Module SHALL include report ID, batch ID, bank ID, reporting date, S3 URIs, file sizes, presigned URLs, overall quality score, compliance status, and generation timestamps
5. WHEN database insert fails, THE Report Generation Module SHALL rollback transaction, leave S3 files intact for recovery, and retry once after 2 seconds

### Requirement 14

**User Story:** As a system integrator, I want the module to publish ReportGeneratedEvent using transactional outbox pattern, so that downstream systems reliably receive report availability notifications even if publication initially fails.

#### Acceptance Criteria

1. WHEN report generation completes, THE Report Generation Module SHALL create Generated_Report aggregate with ReportGeneratedEvent domain event
2. WHEN saving the aggregate, THE Report Generation Module SHALL register it with BaseUnitOfWork using unitOfWork.registerEntity(generatedReport)
3. WHEN calling unitOfWork.saveChanges(), THE Report Generation Module SHALL persist both aggregate and domain events atomically to database in same transaction
4. WHEN OutboxProcessor scheduled job runs, THE Report Generation Module SHALL use ReportGenerationEventPublisher with IIntegrationEventBus to publish events from outbox table
5. WHEN event publication fails, THE Report Generation Module SHALL rely on OutboxProcessor retry mechanism (every 60 seconds, up to 5 retries with exponential backoff) to ensure eventual delivery

### Requirement 15

**User Story:** As a downstream notification system, I want to receive report generation events with necessary data, so that I can send appropriate notifications to users.

#### Acceptance Criteria

1. WHEN report generation completes, THE Report Generation Module SHALL publish ReportGeneratedEvent with report metadata including quality score and compliance status
2. WHEN publishing notification data, THE Report Generation Module SHALL include report ID, batch ID, bank ID, reporting date, report type (COMPREHENSIVE), and presigned URLs
3. WHEN including download links, THE Report Generation Module SHALL provide presigned URLs with 1-hour expiration
4. WHEN event is published, THE Report Generation Module SHALL NOT handle email sending directly (delegated to notification module)
5. WHEN event publishing completes, THE Report Generation Module SHALL log event publication confirmation

### Requirement 16

**User Story:** As a performance engineer, I want the system to complete report generation within 7 seconds, so that users receive timely results.

#### Acceptance Criteria

1. WHEN both events are received, THE Report Generation Module SHALL complete data fetching (both files) in approximately 800 milliseconds
2. WHEN generating HTML report, THE Report Generation Module SHALL complete rendering (including quality recommendations) in approximately 2.0 seconds
3. WHEN generating XBRL report, THE Report Generation Module SHALL complete generation and validation in approximately 800 milliseconds
4. WHEN uploading to S3, THE Report Generation Module SHALL complete both uploads in approximately 800 milliseconds
5. WHEN measuring end-to-end duration, THE Report Generation Module SHALL emit metric report.generation.duration.seconds with target of 5-7 seconds

### Requirement 17

**User Story:** As a system operator, I want comprehensive error handling and recovery mechanisms, so that temporary failures are automatically retried and permanent failures are properly escalated.

#### Acceptance Criteria

1. WHEN data files are not found in S3 after 3 retry attempts, THE Report Generation Module SHALL log CRITICAL error, create database record with FAILED status, and alert operations team
2. WHEN JSON parsing fails, THE Report Generation Module SHALL save malformed file to /tmp/malformed-json/ for analysis, mark status as FAILED, and alert development team
3. WHEN data validation fails with missing required fields or invalid formats, THE Report Generation Module SHALL log specific validation errors, save invalid data for review, and alert data quality team
4. WHEN Thymeleaf template rendering fails, THE Report Generation Module SHALL attempt fallback simplified template and proceed with PARTIAL status if successful
5. WHEN XBRL XSD validation fails, THE Report Generation Module SHALL attempt automatic corrections (trim whitespace, round decimals), and if unsuccessful, proceed with HTML-only report with PARTIAL status

### Requirement 18

**User Story:** As a developer, I want the system to follow DDD principles and use shared infrastructure, so that the module integrates seamlessly with the platform architecture.

#### Acceptance Criteria

1. WHEN implementing domain logic, THE Report Generation Module SHALL follow "ask the object what it can do" principle with behavior encapsulated in domain objects
2. WHEN handling events, THE Report Generation Module SHALL use EventRetryProcessor from regtech-core-application for automatic retry with exponential backoff
3. WHEN persisting event failures, THE Report Generation Module SHALL save EventProcessingFailure records for retry processing
4. WHEN accessing file storage, THE Report Generation Module SHALL use IFileStorageService interface from regtech-core-infrastructure with S3FileStorageService and LocalFileStorageService implementations
5. WHEN organizing code, THE Report Generation Module SHALL follow 4-layer architecture (Domain → Application → Infrastructure → Presentation) with proper dependency flow

### Requirement 19

**User Story:** As a system operator, I want comprehensive monitoring, metrics, and alerting, so that I can track system health, diagnose issues quickly, and respond to incidents proactively.

#### Acceptance Criteria

1. WHEN report generation executes, THE Report Generation Module SHALL emit performance timers for overall duration, data fetch, HTML generation, XBRL generation, S3 upload, and database save with percentiles (p50, p75, p90, p95, p99)
2. WHEN operations complete, THE Report Generation Module SHALL emit counters for success, failure (with failure_reason tags), partial generation, retries, duplicates, and circuit breaker transitions
3. WHEN system resources are monitored, THE Report Generation Module SHALL emit gauges for database connection pool (active/idle), async executor (queue size/active threads), deferred upload count, and circuit breaker state
4. WHEN logging events, THE Report Generation Module SHALL use JSON structured logging with standard fields (timestamp, level, logger, thread, message, batch_id, report_id, bank_id, duration_ms, exception, trace_id)
5. WHEN critical conditions occur, THE Report Generation Module SHALL trigger alerts: CRITICAL (failure rate >10%, S3 consecutive failures, DB pool exhausted, permission denied), HIGH (event timeout rate >20%, deferred uploads accumulating, XBRL validation spike), MEDIUM (P95 duration >10s, partial reports, outbox accumulating)

### Requirement 20

**User Story:** As a system operator, I want S3 upload failures handled gracefully with automatic retry, so that temporary network issues don't result in lost reports.

#### Acceptance Criteria

1. WHEN S3 upload fails due to network timeout or service unavailability, THE Report Generation Module SHALL save EventProcessingFailure record with event payload for automatic retry by EventRetryProcessor
2. WHEN S3 upload fails due to permission denied (403) or bucket not found (404), THE Report Generation Module SHALL log CRITICAL error, alert operations team, and mark status as FAILED without retry
3. WHEN EventRetryProcessor retries failed upload events, THE Report Generation Module SHALL use configured retry options (maxRetries, backoffIntervalsSeconds) from EventRetryOptions
4. WHEN retry count reaches maxRetries limit, THE Report Generation Module SHALL mark event as permanently failed and move to dead letter handling
5. WHEN upload succeeds on retry, THE Report Generation Module SHALL update database status to COMPLETED, generate presigned URLs, and publish ReportGeneratedEvent

### Requirement 21

**User Story:** As a database administrator, I want database transaction failures handled with compensating actions, so that orphaned S3 files are cleaned up and data consistency is maintained.

#### Acceptance Criteria

1. WHEN database insert fails after report files are uploaded to S3, THE Report Generation Module SHALL leave files on S3 (not delete) for easier recovery
2. WHEN database insert fails, THE Report Generation Module SHALL retry once after 2 seconds before marking as permanently failed
3. WHEN database insert permanently fails, THE Report Generation Module SHALL create fallback record in report_metadata_failures table with S3 URIs for later reconciliation
4. WHEN scheduled orphaned file cleanup job runs daily, THE Report Generation Module SHALL identify S3 files without corresponding database records and delete files older than 7 days
5. WHEN reconciliation job runs, THE Report Generation Module SHALL attempt to insert records from report_metadata_failures table into generated_reports table

### Requirement 22

**User Story:** As a system operator, I want circuit breaker pattern for S3 operations, so that cascading failures are prevented when S3 service is degraded.

#### Acceptance Criteria

1. WHEN S3 operations experience 10 consecutive failures OR failure rate exceeds 50% over 5-minute window, THE Report Generation Module SHALL open circuit breaker using Resilience4j
2. WHEN circuit breaker is OPEN, THE Report Generation Module SHALL block S3 operations immediately, save files to local filesystem, and emit metric report.s3.circuit.breaker.open
3. WHEN circuit breaker remains OPEN for 5 minutes, THE Report Generation Module SHALL transition to HALF_OPEN state and allow 1 test operation
4. WHEN test operation in HALF_OPEN state succeeds, THE Report Generation Module SHALL close circuit breaker and resume normal S3 operations
5. WHEN circuit breaker closes, THE Report Generation Module SHALL emit metric report.s3.circuit.breaker.closed and allow deferred uploads to proceed

### Requirement 23

**User Story:** As a system developer, I want idempotency guarantees at all levels, so that duplicate event processing or retries produce consistent results without side effects.

#### Acceptance Criteria

1. WHEN duplicate BatchCalculationCompletedEvent or BatchQualityCompletedEvent arrives with same event ID, THE Report Generation Module SHALL detect duplicate and skip processing without error
2. WHEN report generation is triggered for batch_id that already has COMPLETED status, THE Report Generation Module SHALL skip regeneration and return existing record
3. WHEN report generation is triggered for batch_id with FAILED or PARTIAL status, THE Report Generation Module SHALL allow regeneration and overwrite previous record
4. WHEN S3 PutObject is called with same key and content, THE Report Generation Module SHALL rely on S3 natural idempotency with versioning enabled
5. WHEN database insert encounters UNIQUE constraint violation on report_id, THE Report Generation Module SHALL catch exception, query existing record, and proceed without error

### Requirement 24

**User Story:** As a quality assurance engineer, I want comprehensive testing strategy with health checks, so that I can ensure system reliability and quickly identify issues.

#### Acceptance Criteria

1. WHEN implementing unit tests, THE Report Generation Module SHALL achieve ≥85% line coverage and ≥75% branch coverage using JUnit 5 and Mockito for all components (event listeners, generators, uploaders, coordinators)
2. WHEN implementing integration tests, THE Report Generation Module SHALL use Spring Boot Test with Testcontainers (PostgreSQL, LocalStack S3) to validate complete flows including happy path, reverse event order, duplicates, failures, and partial generation
3. WHEN implementing health checks, THE Report Generation Module SHALL provide Spring Boot Actuator endpoints for liveness (/actuator/health/liveness) and readiness (/actuator/health/readiness) with custom indicators for database, S3, event coordinator, and async executor
4. WHEN health indicators execute, THE Report Generation Module SHALL return UP status when all components healthy, WARN status for degraded performance (queue 50-80%, slow S3 response), and DOWN status for failures (DB timeout, S3 inaccessible, queue full, >50 pending events >5min old)
5. WHEN end-to-end testing is performed, THE Report Generation Module SHALL validate complete pipeline from upstream event publication through report generation to file download with manual verification of HTML rendering and XBRL validation
