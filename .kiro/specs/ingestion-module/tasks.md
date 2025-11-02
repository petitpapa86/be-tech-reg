# Implementation Plan

- [x] 1. Set up ingestion module structure leveraging existing core infrastructure





  - Create regtech-ingestion module with Maven structure
  - Set up dependencies on regtech-core for Result<T>, ErrorDetail, CrossModuleEventBus, outbox patterns
  - Create directory structure: domain, application, infrastructure, web packages
  - _Requirements: 11.1, 11.4, 12.6_

- [x] 2. Implement domain layer with IngestionBatch aggregate





  - [x] 2.1 Create value objects and enums


    - Implement BatchId, BankId value objects
    - Create BatchStatus enum (UPLOADED, PARSING, VALIDATED, STORING, COMPLETED, FAILED)
    - Create FileMetadata record with validation logic
    - _Requirements: 1.5, 2.1, 12.1_



  - [x] 2.2 Create IngestionBatch aggregate root extending Entity





    - Implement business behavior methods: startProcessing(), markAsValidated(), attachBankInfo(), recordS3Storage(), completeIngestion()
    - Add state transition validation with canTransitionTo() method
    - Use existing Entity base class for domain events collection


    - _Requirements: 12.1, 12.4, 12.7_

  - [x] 2.3 Implement Specification Pattern for business rules


    - Create IngestionBatchSpecifications with canBeProcessed(), mustBeParsed(), canBeStored(), mustNotBeFailed()
    - Implement combinable specifications with .and() and .or() operators
    - _Requirements: 11.6, 12.2, 12.5_

  - [ ] 2.4 Create BatchTransitions for state validation
    - Implement validateTransition() method with valid state change rules
    - Use existing Result<T> and ErrorDetail from regtech-core
    - _Requirements: 11.7, 12.3, 12.6_

  - [ ]* 2.5 Write unit tests for domain aggregate behavior
    - Test IngestionBatch state transitions and business methods
    - Test Specification Pattern combinations and edge cases
    - Verify domain events are raised correctly
    - _Requirements: 12.1, 12.2, 12.3_
-

- [x] 3. Create database schema and JPA entities





  - [x] 3.1 Create database migration scripts





    - Create ingestion_batches table with all required fields and indexes
    - Create bank_info table for caching bank information
    - Leverage existing outbox table from regtech-core
    - _Requirements: 5.1, 3.4, 6.4_

  - [x] 3.2 Implement JPA entities


    - Create IngestionBatchEntity mapping to ingestion_batches table
    - Implement BankInfoEntity for bank_info cache table
    - Create S3Reference record for storage references
    - _Requirements: 5.1, 3.2, 4.4_



  - [ ] 3.3 Implement repository interfaces and JPA implementations
    - Create IngestionBatchRepository with findByBatchId(), save(), and query methods
    - Implement BankInfoRepository with caching and timestamp validation
    - Leverage existing outbox infrastructure from regtech-core
    - _Requirements: 5.8, 3.2, 6.5_

  - [ ]* 3.4 Write unit tests for repository operations
    - Test repository CRUD operations with testcontainers
    - Verify database constraints and indexing
    - Test query performance with sample data
    - _Requirements: 5.1, 5.8, 9.7_
-

- [x] 4. Implement file processing services




  - [x] 4.1 Create FileParsingService for JSON and Excel parsing


    - Implement parseJsonFile() with streaming JSON parsing for large files
    - Create parseExcelFile() method reading first worksheet only
    - Add validation for required fields: exposure_id, amount, currency, country, sector
    - _Requirements: 2.2, 2.3, 2.9, 9.2_



  - [ ] 4.2 Implement FileValidationService for business rule validation
    - Validate currency codes against ISO 4217 standard
    - Verify country codes against ISO 3166 standard
    - Check sector codes against internal enumeration
    - Detect duplicate exposure_id values within file


    - _Requirements: 2.6, 2.7, 2.8, 2.5_

  - [ ] 4.3 Create file size and content type validation
    - Validate file size is less than 500MB
    - Accept application/json and Excel content types only
    - Generate appropriate error messages for validation failures
    - _Requirements: 1.1, 1.2, 1.7_

  - [ ]* 4.4 Write unit tests for file parsing and validation
    - Test JSON and Excel parsing with various file formats
    - Verify validation rules with invalid data samples
    - Test streaming parsing with large file scenarios
    - _Requirements: 2.2, 2.3, 9.2_

- [x] 5. Implement bank information enrichment





  - [x] 5.1 Create BankInfoEnrichmentService


    - Check bank_info table for existing data with freshness validation (< 24 hours)
    - Call Bank Registry service when data is stale or missing
    - Store retrieved bank information with timestamp
    - _Requirements: 3.1, 3.2, 3.4_

  - [x] 5.2 Implement BankRegistryClient for external service integration


    - Create HTTP client for Bank Registry service calls
    - Handle service unavailability with fallback to cached data
    - Implement retry logic for transient failures
    - _Requirements: 3.3, 3.6, 8.3_



  - [ ] 5.3 Add bank status validation
    - Reject uploads when bank status is INACTIVE
    - Validate bank registration before processing
    - Generate appropriate error messages for bank validation failures
    - _Requirements: 3.5, 3.7_

  - [ ]* 5.4 Write integration tests for bank enrichment
    - Test bank info caching and freshness validation
    - Mock Bank Registry service responses
    - Verify error handling for inactive banks
    - _Requirements: 3.1, 3.2, 3.7_

- [x] 6. Create S3 storage service with enterprise features





  - [x] 6.1 Implement S3StorageService with production features


    - Use bucket "regtech-data-storage" with "raw/" prefix
    - Enable server-side encryption with AES-256
    - Calculate MD5 and SHA-256 checksums for integrity verification
    - _Requirements: 4.1, 4.2, 4.5_



  - [ ] 6.2 Add multipart upload and metadata handling
    - Use multipart upload for files exceeding 100MB
    - Set metadata: batch-id, bank-id, exposure-count, upload-timestamp
    - Verify ETag matches calculated MD5 checksum


    - _Requirements: 4.6, 4.4, 4.7_

  - [ ] 6.3 Implement retry logic and error handling
    - Retry S3 uploads with exponential backoff (3 attempts maximum)
    - Set lifecycle policy to GLACIER after 180 days
    - Handle S3 service failures gracefully
    - _Requirements: 4.8, 4.9, 8.1_

  - [ ]* 6.4 Write integration tests for S3 operations
    - Test S3 upload with localstack
    - Verify encryption and metadata settings
    - Test multipart upload for large files
    - _Requirements: 4.1, 4.2, 4.6_

- [x] 7. Implement cross-module event publishing leveraging existing infrastructure





  - [x] 7.1 Create BatchIngestedEvent extending IntegrationEvent


    - Include batch_id, bank_id, s3_uri, total_exposures, file_size_bytes in event payload
    - Extend existing IntegrationEvent base class from regtech-core
    - Add event versioning for backward compatibility
    - _Requirements: 6.1, 6.2, 11.9_

  - [x] 7.2 Create IngestionOutboxEventPublisher implementing OutboxEventPublisher


    - Store BatchIngestedEvent in existing outbox table with PENDING status
    - Implement processPendingEvents() and retryFailedEvents() methods
    - Use existing CrossModuleEventBus for event delivery
    - _Requirements: 6.4, 6.5, 6.6_



  - [ ] 7.3 Create IngestionOutboxProcessor extending GenericOutboxEventProcessor
    - Extend existing GenericOutboxEventProcessor from regtech-core
    - Configure for ingestion context with appropriate scheduling
    - Leverage existing retry and monitoring infrastructure
    - _Requirements: 6.5, 6.6_

  - [ ]* 7.4 Write integration tests for event publishing
    - Test outbox pattern with database transactions
    - Verify event delivery and retry mechanisms
    - Test event payload structure and versioning
    - _Requirements: 6.4, 6.5, 6.6_

- [x] 8. Create application layer command and query handlers





  - [x] 8.1 Implement UploadFileCommand and UploadFileCommandHandler


    - Create command record with file data and bank authentication
    - Validate JWT token and extract bank ID using existing security infrastructure
    - Apply rate limiting per bank to prevent abuse
    - Create IngestionBatch aggregate and store initial record
    - Generate unique batch ID with format "batch_YYYYMMDD_HHMMSS_UUID"
    - Return Result<BatchId> using existing Result pattern
    - _Requirements: 1.3, 1.4, 1.5_



  - [ ] 8.2 Create ProcessBatchCommand and ProcessBatchCommandHandler for asynchronous processing
    - Parse file content using FileParsingService
    - Validate structure and business rules
    - Enrich with bank information
    - Store in S3 with enterprise features
    - Update batch record and publish events using existing CrossModuleEventBus
    - Return Result<Void> with structured error handling


    - _Requirements: 2.1, 2.10, 2.11_

  - [ ] 8.3 Implement BatchStatusQuery and BatchStatusQueryHandler
    - Create query record with batch ID and bank authentication
    - Return current batch status and processing stage
    - Provide estimated completion time for in-progress batches
    - Include processing duration and performance metrics
    - Use existing Result pattern for consistent error handling
    - _Requirements: 7.1, 7.2, 7.6_

  - [ ]* 8.4 Write unit tests for command and query handlers
    - Test command validation and business logic
    - Verify error handling and Result pattern usage
    - Test query response formatting
    - _Requirements: 1.3, 2.1, 7.1_

- [ ] 9. Build REST API layer leveraging existing infrastructure
  - [ ] 9.1 Create IngestionController extending BaseController
    - Extend existing BaseController from regtech-core for consistent response handling
    - Implement POST /api/v1/ingestion/upload endpoint
    - Validate file size, content type, and JWT authentication using existing security infrastructure
    - Return HTTP 202 Accepted with batch ID and status URL using existing ApiResponse
    - Handle file too large scenarios with HTTP 413 response
    - _Requirements: 1.1, 1.2, 1.6, 1.7_

  - [ ] 9.2 Implement batch status endpoint
    - Create GET /api/v1/ingestion/batch/{batchId}/status endpoint
    - Return current status, progress, and estimated completion time
    - Include links to download results when processing completes
    - Provide detailed error messages and remediation suggestions
    - Use existing ResponseUtils for consistent response formatting
    - _Requirements: 7.1, 7.4, 7.5_

  - [ ] 9.3 Add error handling leveraging existing infrastructure
    - Use existing global exception handling from regtech-core
    - Format validation errors using existing ErrorDetail and FieldError classes
    - Return structured error responses using existing ApiResponse format
    - _Requirements: 8.2, 8.6_

  - [ ]* 9.4 Write integration tests for REST endpoints
    - Test file upload with various scenarios
    - Verify status endpoint responses
    - Test authentication and authorization
    - _Requirements: 1.1, 1.6, 7.1_

- [ ] 10. Implement monitoring and observability leveraging existing infrastructure
  - [ ] 10.1 Add ingestion-specific metrics collection
    - Emit metrics for file size, processing time, and success rates
    - Track S3 upload/download success rates and latencies
    - Monitor database query performance and connection pool usage
    - Use existing logging configuration and structured logging patterns
    - _Requirements: 13.1, 13.4, 13.5_

  - [ ] 10.2 Create structured logging using existing LoggingConfiguration
    - Use existing LoggingConfiguration.createStructuredLog() for consistent logging
    - Log structured error information with correlation IDs using existing CorrelationId
    - Mask or exclude PII from log entries following existing patterns
    - Provide detailed trace information for request flows
    - _Requirements: 13.2, 10.6, 13.7_

  - [ ] 10.3 Implement health checks extending existing health infrastructure
    - Create health endpoints for database connectivity
    - Check S3 service availability
    - Monitor Bank Registry service health
    - Verify outbox processor status using existing health check patterns
    - _Requirements: 13.6_

  - [ ]* 10.4 Write tests for monitoring components
    - Test metrics collection and emission
    - Verify health check endpoints
    - Test structured logging format
    - _Requirements: 13.1, 13.6, 13.7_

- [ ] 11. Add security and compliance features leveraging existing infrastructure
  - [ ] 11.1 Implement JWT authentication and authorization using existing security infrastructure
    - Use existing JWT validation from regtech-core security package
    - Validate JWT tokens and extract bank ID
    - Verify bank permissions for file access
    - Log all access attempts for audit trail using existing structured logging
    - _Requirements: 10.3, 10.4_

  - [ ] 11.2 Create audit logging system using existing patterns
    - Maintain immutable logs of all processing steps using existing LoggingConfiguration
    - Record processing duration and performance metrics
    - Generate detailed processing audit reports
    - Use existing correlation ID and structured logging patterns
    - _Requirements: 10.4, 10.7_

  - [ ] 11.3 Implement data retention and lifecycle policies
    - Configure S3 lifecycle policies per regulatory requirements
    - Manage data retention according to compliance needs
    - Provide compliance reporting capabilities
    - _Requirements: 10.5, 10.7_

  - [ ]* 11.4 Write security tests
    - Test JWT token validation
    - Verify audit trail completeness
    - Test data encryption verification
    - _Requirements: 10.1, 10.3, 10.4_

- [ ] 12. Create error recovery and resilience mechanisms
  - [ ] 12.1 Implement comprehensive error handling
    - Handle S3 upload failures with exponential backoff retry
    - Provide detailed JSON parsing errors with line numbers
    - Manage Bank Registry unavailability with cached fallback
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 12.2 Add transaction rollback and cleanup
    - Rollback database changes on transaction failures
    - Clean up partial S3 uploads on processing failures
    - Detect file corruption via checksum mismatch
    - _Requirements: 8.4, 8.6_

  - [ ] 12.3 Create system recovery mechanisms
    - Resume processing from last successful checkpoint
    - Handle outbox event delivery failures gracefully
    - Implement circuit breaker for external service calls
    - _Requirements: 8.7, 6.4_

  - [ ]* 12.4 Write resilience tests
    - Test failure scenarios and recovery mechanisms
    - Verify transaction rollback behavior
    - Test circuit breaker functionality
    - _Requirements: 8.1, 8.4, 8.7_

- [ ] 13. Performance optimization and scalability
  - [ ] 13.1 Optimize file processing performance
    - Implement streaming JSON parsing to avoid memory issues
    - Use parallel processing for multiple concurrent files
    - Optimize database queries with appropriate indexing
    - _Requirements: 9.2, 9.3, 9.7_

  - [ ] 13.2 Add performance monitoring and alerting
    - Set up alerts for processing time thresholds
    - Monitor memory usage during large file parsing
    - Track system performance under high load
    - _Requirements: 9.1, 9.4, 13.3_

  - [ ] 13.3 Implement file splitting suggestions
    - Suggest optimal file splitting strategies for large files
    - Provide guidance when files approach size limits
    - Automatically archive old files to Glacier storage class
    - _Requirements: 9.5, 9.6_

  - [ ]* 13.4 Write performance tests
    - Test large file processing (1M+ exposures)
    - Verify concurrent upload scenarios
    - Test memory usage during streaming parsing
    - _Requirements: 9.1, 9.2, 9.3_

- [ ] 14. Integration and end-to-end testing leveraging existing test infrastructure
  - [ ] 14.1 Create integration test suite using existing test patterns
    - Test complete ingestion workflow from upload to event publishing
    - Verify S3 storage operations with localstack
    - Test database operations with testcontainers following existing patterns
    - Use existing test utilities and configurations from regtech-core
    - _Requirements: All requirements integration_

  - [ ] 14.2 Implement end-to-end scenarios
    - Test successful file processing workflow
    - Verify error handling and recovery scenarios using existing Result pattern
    - Test concurrent processing capabilities
    - Verify integration with existing CrossModuleEventBus and outbox patterns
    - _Requirements: Complete workflow validation_

  - [ ]* 14.3 Create performance and load tests
    - Test system behavior under high load
    - Verify scalability with multiple concurrent uploads
    - Test memory and resource usage patterns
    - Use existing monitoring and logging infrastructure for performance analysis
    - _Requirements: 9.3, 9.4_