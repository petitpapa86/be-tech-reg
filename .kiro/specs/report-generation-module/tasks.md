# Implementation Plan

## Overview

This implementation plan breaks down the Report Generation Module into discrete, manageable coding tasks. Each task builds incrementally on previous tasks. All tests are placed at the end to allow for manual testing first. The plan follows the 4-layer architecture (Domain → Application → Infrastructure → Presentation) and emphasizes the "tell, don't ask" principle.

## Important Notes

- **No Compilation Errors**: Ensure code compiles without errors or warnings at each step
- **Use Lombok and Records**: Use Lombok annotations (@Data, @Getter, @Slf4j, @RequiredArgsConstructor, etc.) and Java records for cleaner code
- **Independent Compilation**: The module must compile independently without requiring other modules built by other agents
- **Java 25 with Preview Features**: Use Java 25 with preview features enabled for records and pattern matching

## Task List

- [x] 1. Set up project structure and Maven modules




  - Create parent POM for regtech-report-generation with packaging=pom
  - Configure Java 25 with preview features enabled
  - Add Lombok dependency in parent dependencyManagement
  - Add jqwik for property-based testing in parent dependencyManagement
  - Create domain submodule with dependencies: regtech-core-domain, Lombok, Jackson
  - Create application submodule with dependencies: domain, regtech-core-application, Spring Boot
  - Create infrastructure submodule with dependencies: application, regtech-core-infrastructure, Thymeleaf, Resilience4j, JPA, AWS SDK S3
  - Create presentation submodule with dependencies: application, Spring Boot Actuator, Micrometer
  - Configure maven-compiler-plugin with Java 25 and preview features
  - Configure maven-surefire-plugin for unit tests
  - Configure maven-failsafe-plugin for integration tests
  - Set up package structure: com.bcbs239.regtech.reportgeneration.{domain|application|infrastructure|presentation}
  - Ensure module compiles independently without requiring other modules
  - _Requirements: 15.5_

- [x] 2. Implement domain layer - shared value objects





  - Create ReportId record with UUID generation (use Java record)
  - Create ReportStatus enum (PENDING, IN_PROGRESS, COMPLETED, PARTIAL, FAILED)
  - Create S3Uri record with validation (use Java record with compact constructor)
  - Create FileSize record with human-readable formatting method (use Java record)
  - Create PresignedUrl record with expiration tracking (use Java record)
  - Create FailureReason record with categorization (use Java record)
  - Create ReportingDate record wrapping LocalDate (use Java record)
  - Create ProcessingTimestamps record with start/complete times (use Java record)
  - Use @NonNull from Lombok where appropriate for null safety
  - Ensure all value objects are immutable
  - _Requirements: 10.4_

- [x] 3. Implement domain layer - report metadata value objects




  - Create HtmlReportMetadata record (S3 URI, file size, presigned URL, timestamp) - use Java record
  - Create XbrlReportMetadata record (S3 URI, file size, presigned URL, validation status) - use Java record
  - _Requirements: 10.4_

- [x] 4. Implement GeneratedReport aggregate root





  - Create GeneratedReport class extending BaseEntity from regtech-core-domain
  - Use @Getter from Lombok for query methods (avoid @Data to maintain encapsulation)
  - Use @Builder(access = AccessLevel.PRIVATE) for internal construction
  - Implement static factory method `create()` following "tell, don't ask"
  - Implement `recordHtmlGeneration()` behavior method
  - Implement `recordXbrlGeneration()` behavior method
  - Implement `markAsCompleted()` with business rule validation
  - Implement `markAsPartial()` and `markAsFailed()` methods
  - Implement query methods `isCompleted()` and `canRegenerate()`
  - Encapsulate completion check logic within aggregate
  - Use private helper methods to avoid exposing internal state
  - _Requirements: 10.1, 10.2, 10.3, 15.1_

- [ ] 5. Implement domain events
  - Create ReportGeneratedEvent record extending BaseDomainEvent (use Java record)
  - Create ReportGenerationFailedEvent record extending BaseDomainEvent (use Java record)
  - Ensure events are raised by GeneratedReport aggregate using raiseEvent() method
  - _Requirements: 11.1, 12.2_

- [ ] 6. Implement domain repository interface
  - Create IGeneratedReportRepository interface
  - Define methods: findByBatchId, findByReportId, save, existsByBatchId
  - _Requirements: 10.1_

- [ ] 7. Implement domain services interfaces
  - Create HtmlReportGenerator interface with generate() method
  - Create XbrlReportGenerator interface with generate() method
  - Create XbrlValidator interface with validate() method
  - _Requirements: 5.1, 7.1, 8.1_

- [ ] 8. Implement infrastructure layer - database entities
  - Create GeneratedReportEntity JPA entity with @Entity, @Table
  - Use @Data from Lombok for getters/setters
  - Use @NoArgsConstructor and @AllArgsConstructor from Lombok
  - Map all fields to report_generation_summaries table
  - Add optimistic locking with @Version
  - Create ReportMetadataFailureEntity for fallback table with Lombok annotations
  - _Requirements: 10.4, 18.3_

- [ ] 9. Implement infrastructure layer - JPA repositories
  - Create SpringDataGeneratedReportRepository interface extending JpaRepository
  - Add query methods: findByBatchId, existsByBatchId
  - Create JpaGeneratedReportRepository implementing IGeneratedReportRepository
  - Implement mapping between GeneratedReport aggregate and GeneratedReportEntity
  - _Requirements: 10.1_

- [ ] 10. Implement infrastructure layer - S3 file storage
  - Create S3ReportStorageService with @Service and @Slf4j from Lombok
  - Use @CircuitBreaker annotation from Resilience4j
  - Use @RequiredArgsConstructor from Lombok for dependency injection
  - Implement uploadHtmlReport() with encryption and metadata
  - Implement uploadXbrlReport() with encryption and metadata
  - Implement generatePresignedUrl() with 1-hour expiration
  - Implement fallback method uploadToLocalFallback()
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 11. Implement infrastructure layer - local file storage fallback
  - Create LocalFileStorageService for deferred uploads
  - Implement saveToLocal() method writing to /tmp/deferred-reports/
  - Create scheduled job for retrying deferred uploads
  - _Requirements: 9.5, 17.1_

- [ ] 12. Implement infrastructure layer - circuit breaker configuration
  - Create Resilience4jConfiguration for S3 operations
  - Configure failure threshold: 10 consecutive failures OR 50% over 5 minutes
  - Configure wait duration: 5 minutes in OPEN state
  - Configure permitted calls in HALF_OPEN: 1
  - Add metrics emission for circuit breaker state transitions
  - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_

- [ ] 13. Implement infrastructure layer - Thymeleaf configuration
  - Create ThymeleafConfiguration with template resolver
  - Configure template location: classpath:/templates/reports/
  - Enable caching in production, disable in development
  - Create large-exposures-report.html template with Tailwind CSS
  - Add Chart.js integration for donut and bar charts
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 14. Implement infrastructure layer - HTML report generator
  - Create HtmlReportGeneratorImpl implementing HtmlReportGenerator
  - Implement generate() method using Thymeleaf Context
  - Add header section with bank info, reporting date, regulatory references
  - Add five executive summary cards (Tier 1 Capital, Large Exposures Count, Total Amount, Limit Breaches, Sector Concentration)
  - Add Chart.js data for donut chart (sector distribution) and bar chart (top exposures)
  - Add sortable table with all exposure details
  - Add compliance status with counts and warning badges for >25% exposures
  - Add risk analysis section with concentration risk identification
  - Add footer with generation timestamp and confidentiality notice
  - _Requirements: 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 15. Implement infrastructure layer - XBRL report generator
  - Create XbrlReportGeneratorImpl implementing XbrlReportGenerator
  - Implement generate() method using Java DOM API
  - Add all required EBA COREP namespaces and schema reference
  - Create contexts for each exposure with dimensions (CP, CT, SC)
  - Populate LE1 facts (counterparty name, LEI, identifier type, country, sector)
  - Populate LE2 facts (original amount, amount after CRM, trading/non-trading portions, percentage of capital)
  - Handle missing LEI with CONCAT identifier type
  - Format XML with pretty-print indentation and UTF-8 encoding
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 8.4, 8.5_

- [ ] 16. Implement infrastructure layer - XBRL validator
  - Create XbrlSchemaValidator implementing XbrlValidator
  - Load EBA XSD schema from classpath:/schemas/eba-corep.xsd
  - Implement validate() method returning ValidationResult
  - Return detailed validation errors with line numbers
  - Implement automatic corrections (trim whitespace, round decimals)
  - _Requirements: 8.1, 8.2, 8.5, 14.5_

- [ ] 17. Implement application layer - DTOs and integration events
  - Create CalculationResults record for S3 data (use Java record)
  - Create CalculatedExposure record (use Java record)
  - Create BatchCalculationCompletedEvent record extending BaseIntegrationEvent (use Java record)
  - Create BatchQualityCompletedEvent record extending BaseIntegrationEvent (use Java record)
  - Create ReportGeneratedEvent record extending BaseIntegrationEvent (use Java record)
  - _Requirements: 1.1, 11.1, 12.2_

- [ ] 18. Implement application layer - event coordinator service
  - Create ReportCoordinatorService with @Service and @Slf4j from Lombok
  - Use @RequiredArgsConstructor from Lombok for dependency injection
  - Use ConcurrentHashMap for thread-safe event tracking
  - Implement registerCalculationEvent() method
  - Implement registerQualityEvent() method
  - Implement triggerReportGeneration() when both events received
  - Ensure thread-safety for concurrent event arrival
  - _Requirements: 2.1_

- [ ] 19. Implement application layer - report generation service
  - Create ReportGenerationService with @Service and @Slf4j from Lombok
  - Use @RequiredArgsConstructor from Lombok for dependency injection
  - Implement downloadCalculationResults() using IFileStorageService
  - Implement validateData() with JSON parsing and checksum validation
  - Implement generateHtmlReport() calling HtmlReportGenerator
  - Implement generateXbrlReport() calling XbrlReportGenerator
  - Implement uploadReports() calling S3ReportStorageService
  - Implement saveMetadata() using IGeneratedReportRepository
  - Handle errors with appropriate recovery strategies
  - Use try-catch blocks with proper logging
  - _Requirements: 3.1, 3.2, 4.1, 4.2, 4.3, 4.4, 5.1, 7.1, 9.1, 10.1_

- [ ] 20. Implement application layer - event listeners
  - Create BatchCalculationCompletedEventListener with @Component, @Slf4j from Lombok
  - Use @RequiredArgsConstructor from Lombok for dependency injection
  - Use @Async("reportGenerationExecutor") for async processing
  - Use @EventListener annotation for event handling
  - Implement event validation (not null, not stale >24 hours)
  - Implement idempotency check using repository
  - Delegate to ReportCoordinatorService
  - Save EventProcessingFailure on errors
  - Create BatchQualityCompletedEventListener with similar logic
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 21. Implement application layer - event publisher
  - Create ReportGenerationEventPublisher
  - Implement publishReportGenerated() using IIntegrationEventBus
  - Rely on OutboxProcessor for retry and eventual delivery
  - _Requirements: 11.4, 11.5_

- [ ] 22. Implement error handling and recovery
  - Implement retry logic with exponential backoff for transient errors
  - Implement permanent error handling (no retry for 403, 404)
  - Implement fallback template for Thymeleaf rendering failures
  - Implement compensating transaction for database failures
  - Create scheduled job for orphaned file cleanup (delete files >7 days old)
  - Create scheduled job for reconciliation (process report_metadata_failures)
  - _Requirements: 3.4, 14.1, 14.4, 17.2, 18.1, 18.2, 18.3, 18.4, 18.5_

- [ ] 23. Implement presentation layer - health checks
  - Create ReportGenerationHealthIndicator with @Component and @Slf4j from Lombok
  - Use @RequiredArgsConstructor from Lombok for dependency injection
  - Implement HealthIndicator interface
  - Check database connectivity
  - Check S3 accessibility (HEAD request)
  - Check event tracker state (pending events count)
  - Check async executor queue size
  - Return UP, WARN, or DOWN status based on component health
  - _Requirements: 21.3, 21.4_

- [ ] 24. Implement presentation layer - metrics collection
  - Create ReportGenerationMetricsCollector with @Component and @Slf4j from Lombok
  - Use @RequiredArgsConstructor from Lombok for dependency injection
  - Inject MeterRegistry from Micrometer
  - Emit performance timers (overall duration, data fetch, HTML generation, XBRL generation, S3 upload, DB save)
  - Emit counters (success, failure with tags, partial, retries, duplicates, circuit breaker transitions)
  - Emit gauges (DB connection pool, async executor queue/threads, deferred uploads, circuit breaker state)
  - Configure structured JSON logging with standard fields using Logback
  - _Requirements: 16.1, 16.2, 16.3, 16.4_

- [ ] 25. Implement alerting logic
  - Configure alerts for CRITICAL conditions (failure rate >10%, S3 consecutive failures, DB pool exhausted, permission denied)
  - Configure alerts for HIGH conditions (event timeout rate >20%, deferred uploads accumulating, XBRL validation spike)
  - Configure alerts for MEDIUM conditions (P95 duration >10s, partial reports, outbox accumulating)
  - _Requirements: 16.5_

- [ ] 26. Implement performance optimizations
  - Configure async executor thread pool with appropriate size
  - Implement timeout for file downloads (30 seconds)
  - Optimize HTML template rendering
  - Optimize XBRL generation
  - _Requirements: 4.5, 13.1, 13.2, 13.3, 13.4_

- [ ] 27. Create database migration scripts
  - Create V1__Create_report_generation_summaries_table.sql
  - Create V2__Create_report_metadata_failures_table.sql
  - Add indexes on batch_id, bank_id, status
  - _Requirements: 10.1, 18.3_

- [ ] 28. Configure Spring Boot application properties
  - Configure async executor thread pool
  - Configure S3 bucket names and regions
  - Configure circuit breaker thresholds
  - Configure retry policy (maxRetries, backoff intervals)
  - Configure health check thresholds
  - Configure metrics export
  - _Requirements: 17.3_

- [ ] 29. Create README documentation
  - Document module purpose and responsibilities
  - Document architecture and design decisions
  - Document configuration properties
  - Document monitoring and alerting
  - Document troubleshooting guide
  - _Requirements: All_

- [ ] 30. Checkpoint - Manual testing
  - Manually test the complete workflow
  - Verify HTML report renders correctly
  - Verify XBRL validates against schema
  - Test error scenarios
  - Ask the user if questions arise

- [ ] 31. Write unit tests for domain layer
  - Test GeneratedReport aggregate state transitions
  - Test value object validation
  - Test domain event creation
  - Target: ≥85% line coverage, ≥75% branch coverage
  - _Requirements: 21.1_

- [ ] 32. Write property-based tests for core properties
- [ ] 32.1 Write property test for value object validation
  - **Property 34: File name pattern validation**
  - **Validates: Requirements 9.2**

- [ ] 32.2 Write property test for aggregate state transitions
  - **Property 38: Successful generation creates COMPLETED database record**
  - **Property 39: Partial generation creates PARTIAL database record**
  - **Property 40: Failed generation creates FAILED database record**
  - **Validates: Requirements 10.1, 10.2, 10.3**

- [ ] 32.3 Write property test for domain event creation
  - **Property 42: Domain events are raised on completion**
  - **Validates: Requirements 11.1**

- [ ] 32.4 Write property test for repository idempotency
  - **Property 76: Completed batches skip regeneration**
  - **Property 77: Failed batches allow regeneration**
  - **Validates: Requirements 20.2, 20.3**

- [ ] 32.5 Write property test for S3 upload metadata
  - **Property 33: S3 uploads use correct bucket paths**
  - **Property 35: S3 uploads have correct metadata**
  - **Property 36: Presigned URLs have 1-hour expiration**
  - **Validates: Requirements 9.1, 9.3, 9.4**

- [ ] 32.6 Write property test for circuit breaker behavior
  - **Property 71: Circuit breaker opens on failure threshold**
  - **Property 72: Open circuit blocks operations and uses fallback**
  - **Property 73: Circuit transitions to half-open after wait duration**
  - **Property 74: Successful test operation closes circuit**
  - **Validates: Requirements 19.1, 19.2, 19.3, 19.4**

- [ ] 32.7 Write property test for HTML report content
  - **Property 15: HTML report contains required header elements**
  - **Property 16: HTML report contains all five summary cards**
  - **Property 17: HTML report contains required chart configurations**
  - **Property 18: HTML report table contains all required columns**
  - **Property 19: Compliance counts are calculated correctly**
  - **Property 20: Non-compliant exposures have warning badges**
  - **Property 22: HTML report contains footer with timestamp and notice**
  - **Validates: Requirements 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.5**

- [ ] 32.8 Write property test for XBRL structure
  - **Property 23: XBRL contains LE1 and LE2 templates**
  - **Property 24: XBRL contains all required namespaces**
  - **Property 25: XBRL contexts match exposures with all dimensions**
  - **Property 26: XBRL LE1 facts are complete**
  - **Property 27: XBRL LE2 facts are complete**
  - **Property 31: Missing LEI uses CONCAT identifier**
  - **Property 32: Valid XBRL is formatted with UTF-8 encoding**
  - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 8.4, 8.5**

- [ ] 32.9 Write property test for XBRL validation
  - **Property 28: XBRL validation is performed**
  - **Property 29: XBRL validation errors are logged with details**
  - **Property 30: Invalid XBRL is saved and status marked PARTIAL**
  - **Property 55: XBRL validation failures trigger automatic corrections**
  - **Validates: Requirements 8.1, 8.2, 8.3, 14.5**

- [ ] 32.10 Write property test for event coordination
  - **Property 6: Concurrent events for different batches process independently**
  - **Validates: Requirements 2.1**

- [ ] 32.11 Write property test for data validation
  - **Property 10: Malformed JSON is saved and prevents report generation**
  - **Property 11: Missing required fields prevent report generation**
  - **Property 12: Checksum validation failures prevent report generation**
  - **Property 13: Valid data is mapped to DTOs and processed**
  - **Validates: Requirements 4.1, 4.2, 4.3, 4.4**

- [ ] 32.12 Write property test for event listener behavior
  - **Property 1: Event validation accepts valid events and rejects invalid ones**
  - **Property 2: Duplicate events are detected and skipped**
  - **Property 3: Async processing executes on different thread**
  - **Property 4: Stale events are rejected**
  - **Property 5: Failed event processing creates retry records**
  - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

- [ ] 32.13 Write property test for event publication
  - **Property 43: Aggregate and events persist atomically**
  - **Property 44: Outbox events are published**
  - **Property 45: Failed event publication triggers retry**
  - **Property 46: Published events contain all required fields**
  - **Validates: Requirements 11.3, 11.4, 11.5, 12.2**

- [ ] 32.14 Write property test for error handling
  - **Property 9: File download failures trigger retry with exponential backoff**
  - **Property 53: Missing files after retries create FAILED record**
  - **Property 54: Template rendering failures trigger fallback**
  - **Property 61: Transient S3 failures create retry records**
  - **Property 62: Permanent S3 failures skip retry**
  - **Property 66: Database failures leave S3 files intact**
  - **Property 67: Database failures trigger single retry**
  - **Property 68: Permanent database failures create fallback records**
  - **Validates: Requirements 3.4, 14.1, 14.4, 17.1, 17.2, 18.1, 18.2, 18.3**

- [ ] 32.15 Write property test for health check logic
  - **Property 79: Health indicators return correct status**
  - **Validates: Requirements 21.4**

- [ ] 32.16 Write property test for metrics emission
  - **Property 56: Performance timers are emitted**
  - **Property 57: Operation counters are emitted**
  - **Property 58: Resource gauges are emitted**
  - **Property 59: Structured logs contain required fields**
  - **Validates: Requirements 16.1, 16.2, 16.3, 16.4**

- [ ] 32.17 Write property test for alerting
  - **Property 60: Critical conditions trigger appropriate alerts**
  - **Validates: Requirements 16.5**

- [ ] 32.18 Write property test for performance targets
  - **Property 14: File download timeout is enforced**
  - **Property 48: Data fetching completes within performance target**
  - **Property 49: HTML generation completes within performance target**
  - **Property 50: XBRL generation completes within performance target**
  - **Property 51: S3 uploads complete within performance target**
  - **Property 52: End-to-end duration metric is emitted**
  - **Validates: Requirements 4.5, 13.1, 13.2, 13.3, 13.4, 13.5**

- [ ] 33. Write integration tests
  - Set up Testcontainers (PostgreSQL, LocalStack S3)
  - Test happy path: both events arrive, report generates, files uploaded, metadata saved
  - Test reverse event order: quality event before calculation event
  - Test duplicate events: same event arrives multiple times
  - Test S3 failure: S3 unavailable, fallback to local filesystem
  - Test database failure: DB insert fails, compensating transaction
  - Test partial generation: HTML succeeds but XBRL validation fails
  - Test circuit breaker: multiple S3 failures trigger circuit open
  - Test retry success: failed event retries and succeeds
  - Test stale event: event older than 24 hours is rejected
  - _Requirements: 21.2_

- [ ] 34. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
