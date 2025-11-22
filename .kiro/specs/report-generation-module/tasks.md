# Implementation Plan

- [x] 1. Set up project structure and core interfaces



  - Create Maven multi-module structure (domain, application, infrastructure, presentation)
  - Define module dependencies in parent POM
  - Set up shared dependencies (Spring Boot, Lombok, validation)
  - _Requirements: 18.5_




- [x] 2. Implement domain layer - value objects and enums




- [x] 2.1 Create shared value objects


  - Implement ReportId, BatchId, BankId, ReportingDate
  - Implement S3Uri, PresignedUrl, FileSize, FailureReason
  - Implement ProcessingTimestamps with builder methods
  - _Requirements: 13.4_



- [ ] 2.2 Create report-specific value objects
  - Implement HtmlReportMetadata with S3 URI, file size, presigned URL
  - Implement XbrlReportMetadata with validation status
  - Implement AmountEur for monetary values


  - _Requirements: 13.4_

- [x] 2.3 Create enums and status types





  - Implement ReportType enum (COMPREHENSIVE, LARGE_EXPOSURES, DATA_QUALITY)
  - Implement ReportStatus enum (PENDING, IN_PROGRESS, COMPLETED, PARTIAL, FAILED)
  - Implement ComplianceStatus enum with fromScore() method
  - Implement QualityDimension enum with thresholds
  - Implement QualityGrade enum (A-F)
  - _Requirements: 8.1, 8.2, 13.1_

- [x] 3. Implement domain layer - calculation and quality results






- [x] 3.1 Create CalculationResults domain object

  - Implement CalculationResults with all risk metrics
  - Implement CalculatedExposure value object
  - Implement GeographicBreakdown and SectorBreakdown
  - Implement ConcentrationIndices
  - Add getLargeExposures() and getNonCompliantExposures() methods
  - _Requirements: 6.1, 6.5, 7.1_


- [x] 3.2 Create QualityResults domain object

  - Implement QualityResults with dimension scores
  - Implement ExposureResult and ValidationError inner classes
  - Implement calculateOverallScore() method
  - Implement getErrorDistributionByDimension() method
  - Implement getTopErrorTypes() method
  - _Requirements: 8.1, 8.2, 8.3, 9.3_

- [x] 4. Implement domain layer - GeneratedReport aggregate



- [x] 4.1 Create GeneratedReport aggregate root


  - Implement GeneratedReport entity with JPA annotations
  - Implement createComprehensiveReport() factory method
  - Implement markHtmlGenerated() and markXbrlGenerated() methods
  - Implement markCompleted(), markFailed(), markPartial() methods
  - Register domain events (ReportGenerationStartedEvent, ReportGeneratedEvent, ReportGenerationFailedEvent)
  - _Requirements: 13.1, 13.2, 13.3, 14.1_

- [ ] 5. Implement domain layer - event coordination

- [x] 5.1 Create BatchEventTracker component


  - Implement ConcurrentHashMap-based event tracking
  - Implement markRiskComplete() and markQualityComplete() methods
  - Implement areBothComplete() and getBothEvents() methods
  - Implement cleanup() and cleanupExpiredEvents() methods
  - Create BatchEvents inner class
  - _Requirements: 1.4, 1.5, 2.1_

- [ ] 5.2 Create ReportCoordinator domain service




  - Implement handleCalculationCompleted() method
  - Implement handleQualityCompleted() method
  - Integrate with BatchEventTracker
  - Trigger ComprehensiveReportOrchestrator when both events present
  - _Requirements: 1.4, 1.5, 5.1_

- [x] 6. Implement domain layer - repository interfaces





- [x] 6.1 Create IGeneratedReportRepository interface


  - Define findByBatchId() method
  - Define findByReportId() method
  - Define save() method
  - Define existsByBatchId() and existsByBatchIdAndStatus() methods
  - _Requirements: 13.1, 23.2_


- [x] 6.2 Create domain events

  - Implement ReportGenerationStartedEvent
  - Implement ReportGeneratedEvent with all metadata fields
  - Implement ReportGenerationFailedEvent
  - _Requirements: 14.1, 15.1, 15.2_

- [x] 7. Implement application layer - event listeners




- [x] 7.1 Create ReportEventListener


  - Implement @EventListener for BatchCalculationCompletedEvent
  - Implement @EventListener for BatchQualityCompletedEvent
  - Add @Async annotation with named executor
  - Implement event validation logic
  - Implement idempotency check using repository
  - Implement stale event detection (>24 hours)
  - Delegate to ReportCoordinator
  - Handle errors with EventProcessingFailure
  - _Requirements: 1.1, 1.2, 1.3, 1.6, 1.7, 2.2, 2.3, 2.4_

- [-] 8. Implement application layer - data aggregation


- [x] 8.1 Create ComprehensiveReportDataAggregator service



  - Implement fetchAllData() method
  - Implement fetchCalculationData() method using FilePathResolver
  - Implement fetchQualityData() method using FilePathResolver
  - Implement fetchFileContent() with S3/filesystem logic
  - Implement validateDataConsistency() method
  - Implement DTO mapping methods
  - Add performance metrics
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 5.2_

- [x] 9. Implement application layer - quality recommendations



- [x] 9.1 Create QualityRecommendationsGenerator service


  - Implement generateRecommendations() main method
  - Implement generateCriticalSituationSection() for score <60%
  - Implement generateDimensionSpecificSections() for threshold violations
  - Implement generateErrorPatternSections() for top error types
  - Implement generatePositiveAspectsSection() for excellent dimensions
  - Implement generateActionPlanSection() with short/medium/long-term actions
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 10. Implement application layer - comprehensive report orchestrator

- [x] 10.1 Create ComprehensiveReportOrchestrator service



  - Implement generateComprehensiveReport() main method with @Async
  - Implement idempotency check for existing COMPLETED reports
  - Integrate with ComprehensiveReportDataAggregator
  - Create GeneratedReport aggregate
  - Integrate with QualityRecommendationsGenerator
  - Implement parallel HTML and XBRL generation with CompletableFuture
  - Implement handleGenerationFailure() method
  - Add performance metrics and timers
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 16.1, 16.2, 23.2_

- [x] 11. Implement infrastructure layer - database






- [ ] 11.1 Create database schema migration
  - Create V1__Create_generated_reports_table.sql
  - Add report_id, batch_id, bank_id, reporting_date columns
  - Add report_type, status columns
  - Add html_s3_uri, html_file_size, html_presigned_url columns
  - Add xbrl_s3_uri, xbrl_file_size, xbrl_presigned_url columns
  - Add overall_quality_score, compliance_status columns
  - Add generated_at, completed_at, failure_reason columns
  - Add version column for optimistic locking
  - Add indexes on batch_id, bank_id, status
  - Add UNIQUE constraint on batch_id


  - _Requirements: 13.1, 13.2, 13.3, 13.4_

- [ ] 11.2 Create GeneratedReportEntity JPA entity
  - Implement entity with @Entity and @Table annotations


  - Map all columns with appropriate types
  - Add @Version for optimistic locking
  - _Requirements: 13.4_

- [x] 11.3 Create JpaGeneratedReportRepository implementation





  - Implement IGeneratedReportRepository interface
  - Create SpringDataGeneratedReportRepository interface
  - Implement mapping between domain and entity
  - Implement all query methods
  - _Requirements: 13.1, 13.4_

- [x] 12. Implement infrastructure layer - file storage

- [x] 12.1 Create FilePathResolver utility

  - Implement resolveCalculationPath() for S3 and local paths
  - Implement resolveQualityPath() for S3 and local paths
  - Implement isProd() environment detection
  - _Requirements: 3.1, 3.2, 3.3, 3.4_


- [x] 12.2 Create S3ReportStorageService
  - Implement uploadHtmlReport() with encryption and metadata
  - Implement uploadXbrlReport() with encryption and metadata
  - Implement generatePresignedUrl() with 1-hour expiration
  - Add @CircuitBreaker annotation with fallback
  - Implement uploadToLocalFallback() method
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 22.1, 22.2_


- [x] 12.3 Create LocalFileStorageService for fallback


  - Implement saveToLocal() for deferred uploads
  - Implement scheduled retry job for deferred uploads
  - _Requirements: 12.5, 20.1_

- [x] 13. Implement infrastructure layer - HTML generation




- [x] 13.1 Create Thymeleaf configuration


  - Configure ThymeleafConfiguration bean
  - Set template resolver for classpath:/templates/reports/
  - Enable caching in production, disable in development
  - _Requirements: 6.1_

- [x] 13.2 Create comprehensive HTML template


  - Create comprehensive-report.html template
  - Implement header section with bank info and quality badge
  - Implement Large Exposures section with summary cards
  - Implement Chart.js donut chart for sector distribution
  - Implement Chart.js bar chart for top exposures
  - Implement exposure table with all columns
  - Implement Data Quality section with dimension scores
  - Implement error distribution visualization
  - Implement recommendations sections (dynamic)
  - Implement BCBS 239 compliance section
  - Implement footer with timestamp and notice
  - Apply Tailwind CSS styling
  - _Requirements: 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 13.3 Create HtmlReportGeneratorImpl


  - Implement HtmlReportGenerator interface
  - Create ComprehensiveReportContext with all data
  - Process template with Thymeleaf
  - Return generated HTML string
  - _Requirements: 5.3, 6.1_

- [x] 14. Implement infrastructure layer - XBRL generation



- [x] 14.1 Create XbrlReportGeneratorImpl


  - Implement XbrlReportGenerator interface
  - Create XML document with EBA COREP namespaces
  - Add schema reference to EBA taxonomy
  - Create contexts with dimensions (CP, CT, SC)
  - Populate LE1 facts (counterparty details)
  - Populate LE2 facts (exposure amounts)
  - Format XML with pretty-print
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_


- [x] 14.2 Create XbrlSchemaValidator





  - Implement XbrlValidator interface
  - Load EBA XSD schema from classpath
  - Validate XML document against schema
  - Return ValidationResult with errors
  - Implement automatic corrections (trim, round)
  - _Requirements: 11.1, 11.2, 11.3, 11.5_

- [x] 15. Implement infrastructure layer - circuit breaker



- [x] 15.1 Create Resilience4jConfiguration


  - Configure circuit breaker for S3 operations
  - Set failure threshold: 10 consecutive OR 50% over 5 minutes
  - Set wait duration: 5 minutes
  - Set permitted calls in half-open: 1
  - Emit metrics for state transitions
  - _Requirements: 22.1, 22.2, 22.3, 22.4, 22.5_

- [x] 16. Implement infrastructure layer - event publishing



- [x] 16.1 Create ReportGenerationEventPublisher

  - Implement publishReportGenerated() method
  - Integrate with IIntegrationEventBus
  - Rely on OutboxProcessor for retry
  - _Requirements: 14.4, 14.5, 15.5_

- [x] 17. Implement presentation layer - health checks



- [x] 17.1 Create ReportGenerationHealthIndicator

  - Implement HealthIndicator interface
  - Check database connectivity
  - Check S3 accessibility
  - Check event tracker state
  - Check async executor queue size
  - Return UP, WARN, or DOWN status
  - _Requirements: 24.3, 24.4_

- [x] 18. Implement presentation layer - metrics








- [x] 18.1 Create ReportGenerationMetricsCollector


  - Emit timers for all operations
  - Emit counters for success/failure/partial
  - Emit gauges for resource usage
  - _Requirements: 16.1, 16.2, 16.3, 19.1, 19.2, 19.3_

- [ ] 19. Implement configuration and async executor




- [x] 19.1 Create ReportGenerationConfiguration


  - Configure @Async executor bean "reportGenerationExecutor"
  - Set core pool size, max pool size, queue capacity
  - Configure thread name prefix
  - _Requirements: 1.3, 2.1, 2.2_

- [x] 19.2 Create application-report-generation.yml


  - Configure S3 bucket names and paths
  - Configure file path patterns
  - Configure retry options
  - Configure circuit breaker settings
  - Configure async executor settings
  - _Requirements: 3.1, 3.2, 12.1, 20.3, 22.1_

- [ ] 20. Documentation and deployment preparation

- [-] 20.1 Create module README

  - Document module purpose and architecture
  - Document event flow and coordination
  - Document configuration options
  - Document monitoring and alerting
  - _Requirements: 18.5_

- [ ] 20.2 Create deployment guide
  - Document S3 bucket setup
  - Document database migration steps
  - Document environment variables
  - Document health check endpoints
  - _Requirements: 24.3_

- [ ] 20.3 Create operational runbook
  - Document common failure scenarios
  - Document recovery procedures
  - Document circuit breaker management
  - Document orphaned file cleanup
  - _Requirements: 17.1, 17.2, 21.4, 22.1, 22.2_

- [ ] 21. Testing - Property-based tests
- [ ] 21.1 Write property test for GeneratedReport aggregate
  - **Property 9: Report type is COMPREHENSIVE**
  - **Validates: Requirements 5.1, 13.1**

- [ ] 21.2 Write property test for BatchEventTracker
  - **Property 1: Dual event coordination waits for both events**
  - **Validates: Requirements 1.4, 1.5, 5.1**

- [ ] 21.3 Write property test for event tracker cleanup
  - **Property 5: Event tracker cleanup removes expired events**
  - **Validates: Requirements 1.6**

- [ ] 21.4 Write property test for data aggregator
  - **Property 7: Data aggregator fetches both JSON files**
  - **Validates: Requirements 3.1, 3.2, 5.2**

- [ ] 21.5 Write property test for quality recommendations
  - **Property 3: Quality recommendations are contextual**
  - **Validates: Requirements 9.1, 9.2, 9.3**

- [ ] 21.6 Write property test for recommendation severity
  - **Property 8: Quality score determines recommendation severity**
  - **Validates: Requirements 9.1, 9.2**

- [ ] 21.7 Write property test for parallel generation
  - **Property 6: Parallel generation completes both formats**
  - **Validates: Requirements 5.5, 16.2, 16.3**

- [ ] 21.8 Write property test for idempotency
  - **Property 10: Idempotency prevents duplicate generation**
  - **Validates: Requirements 23.2**

- [ ] 21.9 Write property test for comprehensive report content
  - **Property 2: Comprehensive report includes both sections**
  - **Validates: Requirements 5.3, 6.1, 8.1**

- [ ] 22. Testing - Unit tests
- [ ] 22.1 Write unit tests for event listeners
  - Test valid event processing
  - Test duplicate event detection
  - Test stale event rejection
  - Test error handling with EventProcessingFailure
  - _Requirements: 1.1, 1.2, 1.6, 1.7, 24.1_

- [ ] 22.2 Write unit tests for data aggregation
  - Test S3 file fetching in production mode
  - Test local filesystem fetching in development mode
  - Test JSON parsing and DTO mapping
  - Test data consistency validation
  - Test error handling for malformed JSON
  - Test error handling for missing fields
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 24.1_

- [ ] 22.3 Write unit tests for recommendations generator
  - Test critical situation generation for score <60%
  - Test dimension-specific recommendations for each dimension
  - Test error pattern analysis with top 3 errors
  - Test positive aspects generation
  - Test action plan generation
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 24.1_

- [ ] 22.4 Write unit tests for orchestrator
  - Test happy path with both events
  - Test idempotency for existing reports
  - Test parallel generation completion
  - Test partial failure handling
  - Test complete failure handling
  - _Requirements: 5.1, 5.5, 17.4, 17.5, 23.2, 24.1_

- [ ] 22.5 Write unit tests for repository
  - Test save and findByBatchId
  - Test existsByBatchIdAndStatus
  - Test optimistic locking
  - _Requirements: 13.1, 24.1_

- [ ] 22.6 Write unit tests for file storage
  - Test S3 upload with correct metadata
  - Test presigned URL generation
  - Test circuit breaker fallback
  - Test local storage fallback
  - _Requirements: 12.1, 12.3, 12.5, 24.1_

- [ ] 22.7 Write unit tests for HTML generation
  - Test template rendering with sample data
  - Test Chart.js data generation
  - Test compliance badge rendering
  - Test recommendations rendering
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 8.1, 24.1_

- [ ] 22.8 Write unit tests for XBRL generation
  - Test namespace and schema reference
  - Test context creation with dimensions
  - Test LE1 fact population
  - Test LE2 fact population
  - Test validation against schema
  - Test automatic corrections
  - Test handling of missing LEI
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 11.1, 11.4, 11.5, 24.1_

- [ ] 22.9 Write unit tests for event publisher
  - Test event publication to outbox
  - Test integration with OutboxProcessor
  - _Requirements: 14.4, 15.5, 24.1_

- [ ] 22.10 Write unit tests for health indicators
  - Test UP status when all healthy
  - Test WARN status for degraded performance
  - Test DOWN status for failures
  - _Requirements: 24.3, 24.4_

- [ ] 22.11 Write unit tests for metrics collector
  - Test timer emission
  - Test counter emission with tags
  - Test gauge emission
  - _Requirements: 19.1, 19.2, 19.3, 24.1_

- [ ] 23. Testing - Integration tests
- [ ] 23.1 Write integration test for happy path
  - Publish both events
  - Verify comprehensive report generated
  - Verify both files uploaded to S3
  - Verify database record created
  - Verify ReportGeneratedEvent published
  - _Requirements: 5.1, 5.5, 13.1, 14.1, 24.2, 24.5_

- [ ] 23.2 Write integration test for reverse event order
  - Publish quality event first
  - Publish calculation event second
  - Verify report still generates correctly
  - _Requirements: 1.4, 1.5, 24.2_

- [ ] 23.3 Write integration test for duplicate events
  - Publish same event multiple times
  - Verify only one report generated
  - _Requirements: 1.2, 23.1, 24.2_

- [ ] 23.4 Write integration test for S3 failure
  - Simulate S3 unavailability
  - Verify fallback to local filesystem
  - Verify retry on recovery
  - _Requirements: 12.5, 20.1, 20.5, 22.2, 24.2_

- [ ] 23.5 Write integration test for partial generation
  - Simulate XBRL validation failure
  - Verify HTML generated successfully
  - Verify status marked as PARTIAL
  - _Requirements: 11.3, 13.2, 17.5, 24.2_

- [ ] 23.6 Write integration test for quality recommendations
  - Generate report with various quality scores
  - Verify appropriate recommendations generated
  - Verify contextual error-specific guidance
  - _Requirements: 9.1, 9.2, 9.3, 24.2_

- [ ] 23.7 Write integration tests for circuit breaker
  - Test circuit opens on failure threshold
  - Test fallback to local storage when open
  - Test transition to half-open after wait
  - Test circuit closes on successful test
  - _Requirements: 22.1, 22.2, 22.3, 22.4, 24.2_

- [ ] 24. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

