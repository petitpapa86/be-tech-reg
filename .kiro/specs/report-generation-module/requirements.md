# Requirements Document

## Introduction

The Report Generation Module generates HTML and XBRL-XML reports for Large Exposures (Grandi Esposizioni) compliant with CRR Regulation (EU) No. 575/2013 and EBA ITS standards. The module is triggered by dual Spring Application Events from the Risk Calculation Module and Data Quality Module, coordinates their arrival, retrieves calculation and quality results from S3 storage, generates professional reports, and persists metadata for audit and retrieval purposes.

## Glossary

- **Report_Generation_Module**: The bounded context responsible for generating Large Exposures reports in HTML and XBRL formats
- **Risk_Calculation_Module**: Upstream bounded context that calculates risk metrics and publishes BatchCalculationCompletedEvent
- **Data_Quality_Module**: Upstream bounded context that validates data quality and publishes BatchQualityCompletedEvent
- **BatchCalculationCompletedEvent**: Integration event indicating risk calculation completion for a batch
- **BatchQualityCompletedEvent**: Integration event indicating quality validation completion for a batch
- **Generated_Report**: Aggregate root representing a complete report generation with HTML and XBRL outputs
- **Report_Coordinator**: Domain service that coordinates arrival of dual events before triggering generation
- **Large_Exposures**: Credit exposures equal to or exceeding 10% of eligible capital as defined in CRR Article 392
- **CRR**: Capital Requirements Regulation (EU) No. 575/2013
- **EBA**: European Banking Authority
- **XBRL**: eXtensible Business Reporting Language for regulatory reporting
- **ITS**: Implementing Technical Standards (Commission Implementing Regulation EU No 680/2014)
- **COREP**: Common Reporting framework for supervisory reporting
- **File_Storage_Service**: Infrastructure service abstraction for S3 and local filesystem operations
- **LEI**: Legal Entity Identifier (20-character alphanumeric code)
- **ESA_2010**: European System of Accounts 2010 for sector classification
- **Presigned_URL**: Temporary authenticated URL for secure file access without AWS credentials
- **Event_Retry_Processor**: Core infrastructure component for automatic retry of failed event processing
- **Thymeleaf**: Server-side Java template engine for HTML generation
- **Chart_js**: JavaScript library for interactive data visualization

## Requirements

### Requirement 1

**User Story:** As a risk analyst, I want the system to automatically generate reports when risk calculation completes, so that I receive comprehensive analysis without manual intervention.

#### Acceptance Criteria

1. WHEN BatchCalculationCompletedEvent arrives from Risk Calculation Module, THE Report Generation Module SHALL validate the event using event listener pattern
2. WHEN event validation succeeds, THE Report Generation Module SHALL check idempotency to prevent duplicate processing
3. WHEN idempotency check passes, THE Report Generation Module SHALL trigger asynchronous report generation using @Async annotation
4. WHEN event is invalid or stale (older than 24 hours), THE Report Generation Module SHALL log warning and skip processing
5. WHEN event processing fails, THE Report Generation Module SHALL save EventProcessingFailure record for automatic retry by EventRetryProcessor

### Requirement 2

**User Story:** As a system administrator, I want event processing to be reliable and thread-safe, so that the system handles concurrent events correctly.

#### Acceptance Criteria

1. WHEN multiple BatchCalculationCompletedEvents arrive concurrently for different batches, THE Report Generation Module SHALL process each batch independently using dedicated thread pool
2. WHEN event listener receives an event, THE Report Generation Module SHALL use @Async annotation with named executor for async processing
3. WHEN checking for duplicate processing, THE Report Generation Module SHALL query generated_reports table by batch_id for idempotency
4. WHEN event processing encounters errors, THE Report Generation Module SHALL use EventProcessingFailure repository to persist failure details
5. WHEN EventRetryProcessor retries failed events, THE Report Generation Module SHALL reprocess using the same event listener handler

### Requirement 3

**User Story:** As a compliance officer, I want the system to retrieve calculation results from secure storage, so that reports are based on validated and auditable data sources.

#### Acceptance Criteria

1. WHEN running in production mode, THE Report Generation Module SHALL download calculation results from s3://risk-analysis/calculated/calc_batch_{id}.json
2. WHEN running in development mode, THE Report Generation Module SHALL read files from local filesystem /data/calculated/
3. WHEN downloading files from S3, THE Report Generation Module SHALL use IFileStorageService from regtech-core-infrastructure
4. WHEN file download fails, THE Report Generation Module SHALL use EventRetryProcessor from regtech-core-application for retry with exponential backoff
5. WHEN file storage operations are needed, THE Report Generation Module SHALL leverage shared S3FileStorageService and LocalFileStorageService from regtech-core-infrastructure

### Requirement 4

**User Story:** As a data quality manager, I want the system to validate retrieved data integrity, so that reports are only generated from complete and valid data.

#### Acceptance Criteria

1. WHEN JSON parsing fails, THE Report Generation Module SHALL log an error with parse exception details, save malformed JSON for analysis, and not generate a report
2. WHEN required fields are missing from retrieved data, THE Report Generation Module SHALL log an error with specific validation failures and not generate a report
3. WHEN checksum validation fails, THE Report Generation Module SHALL log an error and not generate a report
4. WHEN data validation succeeds, THE Report Generation Module SHALL map JSON to Java DTOs and proceed with report generation
5. WHEN file download exceeds 30 seconds, THE Report Generation Module SHALL timeout and log an error

### Requirement 5

**User Story:** As a risk analyst, I want a professional HTML report with interactive charts, so that I can analyze Large Exposures data effectively for internal decision-making.

#### Acceptance Criteria

1. WHEN generating HTML report, THE Report Generation Module SHALL use Thymeleaf template engine with Tailwind CSS and Chart.js
2. WHEN rendering the report, THE Report Generation Module SHALL include header section with bank information, reporting date, and regulatory references
3. WHEN displaying summary data, THE Report Generation Module SHALL show five executive summary cards for Tier 1 Capital, Large Exposures Count, Total Amount, Limit Breaches, and Sector Concentration
4. WHEN visualizing data, THE Report Generation Module SHALL generate a donut chart for sector distribution and horizontal bar chart for top exposures
5. WHEN presenting detailed data, THE Report Generation Module SHALL include a sortable table with all large exposures showing counterparty, LEI, sector, rating, amount, percentage of capital, and compliance status

### Requirement 6

**User Story:** As a compliance officer, I want the HTML report to include regulatory compliance information, so that I can assess adherence to CRR requirements.

#### Acceptance Criteria

1. WHEN displaying compliance status, THE Report Generation Module SHALL show counts of compliant exposures (≤25% of capital) and non-compliant exposures (>25% of capital)
2. WHEN rendering exposure details, THE Report Generation Module SHALL mark exposures exceeding 25% limit with red warning badges
3. WHEN including regulatory framework section, THE Report Generation Module SHALL reference CRR Articles 392 and 395, and ITS 680/2014
4. WHEN generating risk analysis section, THE Report Generation Module SHALL identify concentration risks and provide recommendations
5. WHEN completing the report, THE Report Generation Module SHALL include footer with generation timestamp and confidentiality notice

### Requirement 7

**User Story:** As a regulatory reporting officer, I want a valid XBRL-XML file conforming to EBA taxonomy, so that I can submit Large Exposures data to supervisory authorities.

#### Acceptance Criteria

1. WHEN generating XBRL file, THE Report Generation Module SHALL create XML conforming to EBA COREP Framework with templates LE1 and LE2
2. WHEN constructing XBRL structure, THE Report Generation Module SHALL include all required namespaces and schema reference to EBA taxonomy
3. WHEN adding exposure contexts, THE Report Generation Module SHALL create one context per large exposure with dimensions for counterparty (CP), country (CT), and sector (SC)
4. WHEN populating LE1 facts, THE Report Generation Module SHALL include counterparty name, LEI code, identifier type, country code, and sector code for each exposure
5. WHEN populating LE2 facts, THE Report Generation Module SHALL include original exposure amount, exposure after CRM, trading book portion, non-trading book portion, and percentage of eligible capital

### Requirement 8

**User Story:** As a data quality manager, I want XBRL validation against EBA schema, so that generated files meet regulatory technical requirements before submission.

#### Acceptance Criteria

1. WHEN XBRL generation completes, THE Report Generation Module SHALL validate the XML document against EBA XSD schema
2. WHEN validation detects errors, THE Report Generation Module SHALL log detailed validation errors with line numbers and error messages
3. WHEN validation fails, THE Report Generation Module SHALL save the invalid XML to filesystem for debugging and mark report status as PARTIAL
4. WHEN LEI code is missing for a counterparty, THE Report Generation Module SHALL use identifier type "CONCAT" with alternate identifier
5. WHEN validation succeeds, THE Report Generation Module SHALL format XML with pretty-print indentation and UTF-8 encoding

### Requirement 9

**User Story:** As a system administrator, I want generated reports stored securely in S3 with versioning, so that reports are durable, auditable, and accessible.

#### Acceptance Criteria

1. WHEN uploading reports to S3, THE Report Generation Module SHALL upload HTML to s3://risk-analysis/reports/html/ and XBRL to s3://risk-analysis/reports/xbrl/
2. WHEN constructing file names, THE Report Generation Module SHALL use pattern Large_Exposures_{ABI}_{YYYY-MM-DD}.{extension}
3. WHEN uploading files, THE Report Generation Module SHALL set Content-Type appropriately, enable AES-256 server-side encryption, and include metadata tags
4. WHEN uploads complete, THE Report Generation Module SHALL generate presigned URLs with 1-hour expiration for temporary download access
5. WHEN S3 upload fails after 3 retry attempts, THE Report Generation Module SHALL save files to local filesystem /tmp/deferred-reports/ and schedule retry

### Requirement 10

**User Story:** As an auditor, I want report metadata persisted in the database, so that I can track report generation history and access reports for compliance audits.

#### Acceptance Criteria

1. WHEN report generation completes successfully, THE Report Generation Module SHALL insert a record in report_generation_summaries table with status COMPLETED
2. WHEN only one file generates successfully, THE Report Generation Module SHALL insert a record with status PARTIAL
3. WHEN report generation fails completely, THE Report Generation Module SHALL insert a record with status FAILED and failure reason
4. WHEN saving metadata, THE Report Generation Module SHALL include report ID, batch ID, bank ID, reporting date, S3 URIs, file sizes, presigned URLs, and generation timestamps
5. WHEN database insert fails, THE Report Generation Module SHALL rollback transaction, initiate compensating transaction to delete S3 files, and retry once after 2 seconds

### Requirement 11

**User Story:** As a system integrator, I want the module to publish ReportGeneratedEvent using outbox pattern, so that downstream systems reliably receive report availability notifications.

#### Acceptance Criteria

1. WHEN report generation completes, THE Report Generation Module SHALL create Generated_Report aggregate with domain event
2. WHEN saving the aggregate, THE Report Generation Module SHALL register it with BaseUnitOfWork using unitOfWork.registerEntity(generatedReport)
3. WHEN calling unitOfWork.saveChanges(), THE Report Generation Module SHALL persist both aggregate and domain events atomically in same transaction
4. WHEN domain events are persisted to outbox, THE Report Generation Module SHALL use ReportGenerationEventPublisher with IIntegrationEventBus for publishing
5. WHEN OutboxProcessor publishes events, THE Report Generation Module SHALL transform domain events to ReportGeneratedIntegrationEvent for cross-module communication

### Requirement 12

**User Story:** As a downstream notification system, I want to receive report generation events with necessary data, so that I can send appropriate notifications to users.

#### Acceptance Criteria

1. WHEN report generation completes, THE Report Generation Module SHALL publish ReportGeneratedEvent with report metadata
2. WHEN publishing notification data, THE Report Generation Module SHALL include report ID, batch ID, bank ID, reporting date, and presigned URLs
3. WHEN including download links, THE Report Generation Module SHALL provide presigned URLs with 1-hour expiration
4. WHEN event is published, THE Report Generation Module SHALL NOT handle email sending directly (delegated to notification module)
5. WHEN event publishing completes, THE Report Generation Module SHALL log event publication confirmation

### Requirement 13

**User Story:** As a performance engineer, I want the system to complete report generation within 5 seconds, so that users receive timely results.

#### Acceptance Criteria

1. WHEN both events are received, THE Report Generation Module SHALL complete data fetching in approximately 500 milliseconds
2. WHEN generating HTML report, THE Report Generation Module SHALL complete rendering in approximately 1.4 seconds
3. WHEN generating XBRL report, THE Report Generation Module SHALL complete generation and validation in approximately 800 milliseconds
4. WHEN uploading to S3, THE Report Generation Module SHALL complete both uploads in approximately 800 milliseconds
5. WHEN measuring end-to-end duration, THE Report Generation Module SHALL emit metric report.generation.duration.seconds with target of 4-5 seconds

### Requirement 14

**User Story:** As a system operator, I want comprehensive error handling and recovery mechanisms, so that temporary failures are automatically retried and permanent failures are properly escalated.

#### Acceptance Criteria

1. WHEN data files are not found in S3 after 3 retry attempts, THE Report Generation Module SHALL log CRITICAL error, create database record with FAILED status, and alert operations team
2. WHEN JSON parsing fails, THE Report Generation Module SHALL save malformed file to /tmp/malformed-json/ for analysis, mark status as FAILED, and alert development team
3. WHEN data validation fails with missing required fields or invalid formats, THE Report Generation Module SHALL log specific validation errors, save invalid data for review, and alert data quality team
4. WHEN Thymeleaf template rendering fails, THE Report Generation Module SHALL attempt fallback simplified template and proceed with PARTIAL status if successful
5. WHEN XBRL XSD validation fails, THE Report Generation Module SHALL attempt automatic corrections (trim whitespace, round decimals), and if unsuccessful, proceed with HTML-only report with PARTIAL status

### Requirement 15

**User Story:** As a developer, I want the system to follow DDD principles and use shared infrastructure, so that the module integrates seamlessly with the platform architecture.

#### Acceptance Criteria

1. WHEN implementing domain logic, THE Report Generation Module SHALL follow "ask the object what it can do" principle with behavior encapsulated in domain objects
2. WHEN handling events, THE Report Generation Module SHALL use EventRetryProcessor from regtech-core-application for automatic retry with exponential backoff
3. WHEN persisting event failures, THE Report Generation Module SHALL save EventProcessingFailure records for retry processing
4. WHEN accessing file storage, THE Report Generation Module SHALL use IFileStorageService interface from regtech-core-infrastructure with S3FileStorageService and LocalFileStorageService implementations
5. WHEN organizing code, THE Report Generation Module SHALL follow 4-layer architecture (Domain → Application → Infrastructure → Presentation) with proper dependency flow


### Requirement 16

**User Story:** As a system operator, I want comprehensive monitoring and metrics, so that I can track system health and diagnose issues quickly.

#### Acceptance Criteria

1. WHEN report generation starts, THE Report Generation Module SHALL emit metric report.generation.started.total with batch_id and bank_id tags
2. WHEN report generation succeeds, THE Report Generation Module SHALL emit counter report.generation.success.total and timer report.generation.duration.seconds
3. WHEN report generation fails, THE Report Generation Module SHALL emit counter report.generation.failure.total with error_type tag (data_not_found, parse_error, validation_error, template_error, xbrl_error)
4. WHEN S3 operations occur, THE Report Generation Module SHALL emit timers report.s3.upload.html.duration.seconds and report.s3.upload.xbrl.duration.seconds
5. WHEN file sizes are recorded, THE Report Generation Module SHALL emit gauges report.file.size.bytes with file_type tag (html, xbrl)


### Requirement 17

**User Story:** As a system operator, I want S3 upload failures handled gracefully with automatic retry, so that temporary network issues don't result in lost reports.

#### Acceptance Criteria

1. WHEN S3 upload fails due to network timeout or service unavailability, THE Report Generation Module SHALL save EventProcessingFailure record with event payload for automatic retry by EventRetryProcessor
2. WHEN S3 upload fails due to permission denied (403) or bucket not found (404), THE Report Generation Module SHALL log CRITICAL error, alert operations team, and mark status as FAILED without retry
3. WHEN EventRetryProcessor retries failed upload events, THE Report Generation Module SHALL use configured retry options (maxRetries, backoffIntervalsSeconds) from EventRetryOptions
4. WHEN retry count reaches maxRetries limit, THE Report Generation Module SHALL mark event as permanently failed and move to dead letter handling
5. WHEN upload succeeds on retry, THE Report Generation Module SHALL update database status to COMPLETED, generate presigned URLs, and publish ReportGeneratedEvent

### Requirement 18

**User Story:** As a database administrator, I want database transaction failures handled with compensating actions, so that orphaned S3 files are cleaned up and data consistency is maintained.

#### Acceptance Criteria

1. WHEN database insert fails after report files are uploaded to S3, THE Report Generation Module SHALL leave files on S3 (not delete) for easier recovery
2. WHEN database insert fails, THE Report Generation Module SHALL retry once after 2 seconds before marking as permanently failed
3. WHEN database insert permanently fails, THE Report Generation Module SHALL create fallback record in report_metadata_failures table with S3 URIs for later reconciliation
4. WHEN scheduled orphaned file cleanup job runs daily, THE Report Generation Module SHALL identify S3 files without corresponding database records and delete files older than 7 days
5. WHEN reconciliation job runs, THE Report Generation Module SHALL attempt to insert records from report_metadata_failures table into report_generation_summaries table
