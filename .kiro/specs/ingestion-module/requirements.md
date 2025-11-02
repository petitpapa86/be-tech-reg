# Ingestion Module Requirements Document

## Introduction

The Ingestion Module is responsible for receiving, validating, processing, and storing bank exposure data files in a scalable, production-ready manner. This module handles the complete ingestion lifecycle from file upload through S3 storage to event publishing for downstream processing. The system is designed to handle files ranging from small datasets (8 exposures) to large enterprise datasets (1M+ exposures) while maintaining data integrity, audit trails, and high availability.

## Requirements

### Requirement 1: File Upload and Initial Validation

**User Story:** As a bank user, I want to upload my exposure data file via REST API, so that my risk data can be processed by the system.

#### Acceptance Criteria

1. WHEN a file upload request is received THEN the system SHALL validate file size is less than 500MB
2. WHEN file content type is checked THEN the system SHALL accept application/json and application/vnd.openxmlformats-officedocument.spreadsheetml.sheet (Excel) files
3. WHEN bank authentication is verified THEN the system SHALL validate JWT token and extract bank ID
4. WHEN rate limiting is applied THEN the system SHALL enforce per-bank upload limits to prevent abuse
5. WHEN file passes initial validation THEN the system SHALL generate unique batch ID with format "batch_YYYYMMDD_HHMMSS_UUID"
6. WHEN upload is accepted THEN the system SHALL return HTTP 202 Accepted with batch ID and status URL
7. WHEN file is too large THEN the system SHALL return HTTP 413 Payload Too Large with split file suggestion

### Requirement 2: Asynchronous File Parsing and Structure Validation

**User Story:** As a system, I want to parse and validate JSON and Excel files asynchronously, so that the upload API remains responsive and can handle large files efficiently.

#### Acceptance Criteria

1. WHEN file parsing begins THEN the system SHALL update batch status to PARSING
2. WHEN parsing JSON files THEN the system SHALL validate required fields: exposure_id, amount, currency, country, sector
3. WHEN parsing Excel files THEN the system SHALL read the first worksheet and validate required columns: exposure_id, amount, currency, country, sector
4. WHEN validating data types THEN the system SHALL ensure amounts are positive numbers and dates are in valid format
5. WHEN checking for duplicates THEN the system SHALL verify no duplicate exposure_id values exist within the file
6. WHEN validating currency codes THEN the system SHALL verify against ISO 4217 standard
7. WHEN validating country codes THEN the system SHALL verify against ISO 3166 standard
8. WHEN validating sector codes THEN the system SHALL verify against internal sector enumeration
9. WHEN Excel file has multiple sheets THEN the system SHALL process only the first sheet and log a warning
10. WHEN validation succeeds THEN the system SHALL update batch status to VALIDATED
11. WHEN validation fails THEN the system SHALL update batch status to FAILED with detailed error messages

### Requirement 3: Bank Information Enrichment

**User Story:** As a system, I want to enrich exposure data with bank information, so that downstream modules have complete context for processing.

#### Acceptance Criteria

1. WHEN bank information is needed THEN the system SHALL first check bank_info table
2. WHEN bank info exists and is fresh (< 24 hours) THEN the system SHALL use stored data
3. WHEN bank info is stale or missing THEN the system SHALL call Bank Registry service
4. WHEN Bank Registry returns bank info THEN the system SHALL store the result in bank_info table with timestamp
5. WHEN bank is not found in registry THEN the system SHALL mark batch as FAILED with "Bank not registered" error
6. WHEN bank info is retrieved THEN the system SHALL attach bank metadata (name, country, status) to batch record
7. WHEN bank status is INACTIVE THEN the system SHALL reject the upload with appropriate error message

### Requirement 4: S3 Storage with Production Features

**User Story:** As a system, I want to store exposure files in S3 with enterprise-grade features, so that data is durable, secure, and compliant with audit requirements.

#### Acceptance Criteria

1. WHEN storing files in S3 THEN the system SHALL use bucket "regtech-data-storage" with prefix "raw/"
2. WHEN uploading to S3 THEN the system SHALL enable server-side encryption with AES-256
3. WHEN file is uploaded THEN the system SHALL enable versioning to maintain audit trail
4. WHEN adding metadata THEN the system SHALL include batch-id, bank-id, exposure-count, and upload-timestamp
5. WHEN calculating checksums THEN the system SHALL generate both MD5 and SHA-256 for integrity verification
6. WHEN file exceeds 100MB THEN the system SHALL use multipart upload for reliability
7. WHEN upload completes THEN the system SHALL verify ETag matches calculated MD5 checksum
8. WHEN S3 upload fails THEN the system SHALL retry with exponential backoff (3 attempts maximum)
9. WHEN storage class is set THEN the system SHALL use STANDARD with lifecycle policy to GLACIER after 180 days

### Requirement 5: Database Batch Record Management

**User Story:** As a system, I want to store batch metadata in the database without storing individual exposures, so that the system scales efficiently while maintaining queryable summaries.

#### Acceptance Criteria

1. WHEN creating batch record THEN the system SHALL store exactly one row per batch in ingestion_batches table
2. WHEN storing S3 reference THEN the system SHALL record s3_uri, s3_bucket, s3_key, and s3_version_id
3. WHEN recording file metadata THEN the system SHALL store total_exposures count, file_size_bytes, and checksums
4. WHEN storing bank information THEN the system SHALL include bank_id, bank_name, and bank_country
5. WHEN setting timestamps THEN the system SHALL record uploaded_at and completed_at with UTC timezone
6. WHEN batch processing completes THEN the system SHALL update status to COMPLETED
7. WHEN individual exposures are processed THEN the system SHALL NOT store them in database (only in S3 file)
8. WHEN querying batch summaries THEN the system SHALL support fast queries on batch metadata without accessing S3

### Requirement 6: Cross-Module Event Publishing

**User Story:** As a system, I want to publish events to notify downstream modules, so that risk calculation, data quality, and billing modules can process the ingested data.

#### Acceptance Criteria

1. WHEN batch ingestion completes THEN the system SHALL publish BatchIngestedEvent via CrossModuleEventBus
2. WHEN creating event payload THEN the system SHALL include batch_id, bank_id, s3_uri, total_exposures, and file_size_bytes
3. WHEN event publishing fails THEN the system SHALL use outbox pattern to ensure guaranteed delivery
4. WHEN outbox pattern is used THEN the system SHALL store event in outbox table with PENDING status
5. WHEN outbox processor runs THEN the system SHALL retry failed events every 30 seconds with exponential backoff
6. WHEN event is successfully delivered THEN the system SHALL update outbox status to PUBLISHED
7. WHEN downstream modules receive event THEN they SHALL download exposure details from provided S3 URI

### Requirement 7: Status Tracking and Progress Monitoring

**User Story:** As a bank user, I want to track the progress of my file processing, so that I know when my data is ready for analysis.

#### Acceptance Criteria

1. WHEN client polls status endpoint THEN the system SHALL return current batch status and processing stage
2. WHEN batch is in progress THEN the system SHALL provide estimated completion time
3. WHEN downstream processing starts THEN the system SHALL track risk_calculation and data_quality progress
4. WHEN all processing completes THEN the system SHALL provide links to download results
5. WHEN errors occur THEN the system SHALL provide detailed error messages and suggested remediation
6. WHEN status is queried THEN the system SHALL return processing duration and performance metrics
7. WHEN batch is completed THEN the system SHALL provide access to generated reports and analysis results

### Requirement 8: Error Handling and Recovery

**User Story:** As a system, I want comprehensive error handling with automatic recovery, so that transient failures don't result in data loss.

#### Acceptance Criteria

1. WHEN S3 upload fails THEN the system SHALL retry with exponential backoff and mark batch as FAILED after 3 attempts
2. WHEN JSON parsing fails THEN the system SHALL provide detailed validation errors with line numbers and field names
3. WHEN Bank Registry is unavailable THEN the system SHALL retry and use stored bank_info data if available
4. WHEN database transaction fails THEN the system SHALL rollback all changes and clean up partial S3 uploads
5. WHEN event publishing fails THEN the system SHALL store event in outbox for guaranteed eventual delivery
6. WHEN file is corrupted THEN the system SHALL detect via checksum mismatch and reject with clear error message
7. WHEN system recovers from failure THEN the system SHALL resume processing from last successful checkpoint

### Requirement 9: Performance and Scalability

**User Story:** As a system administrator, I want the ingestion module to handle varying loads efficiently, so that both small and large banks can use the system effectively.

#### Acceptance Criteria

1. WHEN processing small files (< 1000 exposures) THEN the system SHALL complete ingestion within 2 seconds
2. WHEN processing large files (> 100,000 exposures) THEN the system SHALL use streaming JSON parsing to avoid memory issues
3. WHEN multiple files are uploaded concurrently THEN the system SHALL process them in parallel without resource contention
4. WHEN system is under high load THEN the system SHALL maintain response times under 5 seconds for status queries
5. WHEN file size approaches limits THEN the system SHALL suggest optimal file splitting strategies
6. WHEN storage grows large THEN the system SHALL automatically archive old files to Glacier storage class
7. WHEN database queries are performed THEN the system SHALL use appropriate indexes for fast metadata retrieval

### Requirement 10: Security and Compliance

**User Story:** As a compliance officer, I want the ingestion module to maintain security and audit trails, so that regulatory requirements are met.

#### Acceptance Criteria

1. WHEN files are stored THEN the system SHALL encrypt all data at rest using AES-256 encryption
2. WHEN data is transmitted THEN the system SHALL use TLS 1.3 for all communications
3. WHEN access is granted THEN the system SHALL verify bank permissions and log all access attempts
4. WHEN audit trail is required THEN the system SHALL maintain immutable logs of all processing steps
5. WHEN data retention is managed THEN the system SHALL implement lifecycle policies per regulatory requirements
6. WHEN sensitive data is logged THEN the system SHALL mask or exclude PII from log entries
7. WHEN compliance reporting is needed THEN the system SHALL provide detailed processing audit reports

### Requirement 11: Integration with Modular Architecture and Domain Rules

**User Story:** As a system architect, I want the ingestion module to integrate seamlessly with the modular monolithic architecture using explicit business rules, so that it follows DDD principles and maintains clean boundaries.

#### Acceptance Criteria

1. WHEN implementing domain logic THEN the system SHALL use Result<T> pattern instead of throwing exceptions
2. WHEN handling cross-module communication THEN the system SHALL use CrossModuleEventBus for loose coupling
3. WHEN storing domain events THEN the system SHALL use outbox pattern for guaranteed delivery
4. WHEN implementing repositories THEN the system SHALL follow hexagonal architecture with domain interfaces
5. WHEN processing commands THEN the system SHALL use CQRS pattern with separate command and query handlers
6. WHEN validating complex business rules THEN the system SHALL use Specification Pattern with explicit rule classes (e.g., IngestionBatchSpecifications.canBeProcessed())
7. WHEN managing batch state transitions THEN the system SHALL use Transition Rules pattern to validate and apply status changes (e.g., BatchTransitions.validateTransition())
8. WHEN encapsulating business logic THEN the system SHALL create domain aggregates (e.g., IngestionBatch) with behavior methods that read like domain language
9. WHEN publishing integration events THEN the system SHALL maintain event versioning for backward compatibility
10. WHEN domain rules become complex THEN the system SHALL make them explicit, reusable, and testable through dedicated specification classes

### Requirement 12: Domain Aggregate Design for IngestionBatch

**User Story:** As a domain expert, I want the IngestionBatch to be modeled as a proper domain aggregate with explicit business rules, so that batch processing logic is clear and maintainable.

#### Acceptance Criteria

1. WHEN creating IngestionBatch aggregate THEN the system SHALL encapsulate batch state and behavior in a single aggregate root
2. WHEN validating batch transitions THEN the system SHALL use IngestionBatchSpecifications (e.g., mustBeParsed(), canBeStored(), mustNotBeFailed())
3. WHEN changing batch status THEN the system SHALL use BatchTransitions.validateTransition() to ensure valid state changes
4. WHEN implementing batch behavior THEN the system SHALL provide domain methods like startProcessing(), markAsValidated(), completeIngestion()
5. WHEN business rules are complex THEN the system SHALL create explicit specification classes that can be combined with .and() and .or() operators
6. WHEN batch invariants are violated THEN the system SHALL return Result.failure() with structured ErrorDetail instead of throwing exceptions
7. WHEN batch processing succeeds THEN the system SHALL raise domain events (e.g., BatchValidatedEvent, BatchStoredEvent) for cross-module communication

### Requirement 13: Monitoring and Observability

**User Story:** As a DevOps engineer, I want comprehensive monitoring of the ingestion module, so that I can proactively identify and resolve issues.

#### Acceptance Criteria

1. WHEN processing files THEN the system SHALL emit metrics for file size, processing time, and success rates
2. WHEN errors occur THEN the system SHALL log structured error information with correlation IDs
3. WHEN performance degrades THEN the system SHALL alert on processing time thresholds
4. WHEN S3 operations are performed THEN the system SHALL track upload/download success rates and latencies
5. WHEN database operations are performed THEN the system SHALL monitor query performance and connection pool usage
6. WHEN system health is checked THEN the system SHALL provide health endpoints for load balancer integration
7. WHEN troubleshooting is needed THEN the system SHALL provide detailed trace information for request flows