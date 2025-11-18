# Implementation Plan

- [x] 1. Set up project structure and core interfaces
  - Create Maven multi-module structure with parent POM and 4 layer POMs
  - Set up directory structure following established conventions
  - Configure dependencies between layers according to Clean Architecture
  - Set up basic Spring Boot configuration and profiles
  - _Requirements: 1.1, 6.4_

- [x] 2. Implement domain layer foundations
- [x] 2.1 Create shared domain foundation
  - Create domain/shared/enums/ with GeographicRegion, SectorCategory, CalculationStatus
  - Create domain/shared/valueobjects/ with records (BatchId, BankId, AmountEur, ExchangeRate, etc.)
  - Add validation logic in record constructors for fail-fast behavior
  - Create domain/shared/exceptions/ for domain-specific exceptions
  - _Requirements: 3.1, 4.1, 2.1_

- [x] 2.2 Create calculation capability domain
  - Create domain/calculation/BatchSummary aggregate root using Lombok
  - Add DDD behavior methods (startCalculation(), completeCalculation(), failCalculation())
  - Create domain/calculation/CalculatedExposure entity using Lombok
  - Add DDD behavior methods (convertCurrency(), classify(), calculatePercentage())
  - Create domain/calculation/events/ for domain events
  - Create domain/calculation/IBatchSummaryRepository interface
  - _Requirements: 6.1, 6.2, 2.3_

- [x] 2.3 Create classification and aggregation capabilities
  - Create domain/classification/GeographicClassifier with EU country mapping
  - Create domain/classification/SectorClassifier with sector code mapping (use switch expressions)
  - Create domain/classification/ClassificationRules for business rules
  - Create domain/aggregation/HerfindahlIndex as record with static calculate() method
  - Create domain/aggregation/ConcentrationCalculator for all concentration metrics
  - Add ConcentrationLevel enum and business logic methods
  - _Requirements: 2.1, 3.1, 4.1, 5.1_

- [x] 2.4 Create domain service interfaces
  - Create domain/services/CurrencyConversionService interface
  - Create domain/services/GeographicClassificationService interface  
  - Create domain/services/SectorClassificationService interface
  - _Requirements: 2.1, 3.1, 4.1_

- [x] 3. Implement application layer commands and handlers
- [x] 3.1 Create calculation capability commands
  - Create application/calculation/CalculateRiskMetricsCommand as record with validation
  - Add static factory method create() with Result-based validation
  - Use Maybe<String> for optional correlationId parameter
  - Create application/calculation/CalculateRiskMetricsCommandHandler using Lombok
  - Create application/calculation/RiskCalculationService for orchestration
  - _Requirements: 1.1, 1.4_

- [x] 3.2 Create classification capability commands
  - Create application/classification/ClassifyExposuresCommand and handler
  - Use DDD approach: ask domain classifiers to do the work
  - Create application/classification/GeographicClassificationService
  - Create application/classification/SectorClassificationService
  - _Requirements: 3.1, 4.1_

- [x] 3.3 Create aggregation capability commands
  - Create application/aggregation/CalculateAggregatesCommand and handler
  - Use DDD approach: ask ConcentrationCalculator to calculate HHI
  - Create application/aggregation/ConcentrationCalculationService
  - Implement summary statistics calculation logic
  - _Requirements: 5.1, 6.1_

- [x] 3.4 Create integration capability
  - Create application/integration/BatchIngestedEventListener with async processing
  - Add idempotency checking and duplicate detection
  - Implement event filtering and validation
  - Add error handling with EventProcessingFailure repository
  - Create application/integration/BatchCompletedIntegrationAdapter (similar to data-quality)
  - _Requirements: 1.1, 1.3, 1.4, 1.5_

- [x] 3.5 Create shared application services
  - Create application/shared/FileProcessingService for S3/filesystem operations
  - Create application/shared/CurrencyConversionService with external rate provider
  - Add caching and error handling for currency conversion
  - _Requirements: 2.1, 2.5_

- [x] 3.6 Create integration event publishers
  - Create application/integration/RiskCalculationEventPublisher
  - Implement BatchCalculationCompletedEvent and BatchCalculationFailedEvent
  - Add event content validation and structured logging
  - Use transactional event listeners for reliable publishing
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 4. Implement infrastructure layer
- [x] 4.1 Create database persistence
  - Create infrastructure/database/entities/BatchSummaryEntity using Lombok
  - Use @Entity, @Table, @Getter, @Setter, @Builder, @NoArgsConstructor/@AllArgsConstructor
  - Create infrastructure/database/repositories/JpaBatchSummaryRepository
  - Implement Result-based methods following established patterns
  - Create infrastructure/database/mappers/ for domain ↔ entity conversion
  - Create database migration scripts in src/main/resources/db/migration/ for batch_summaries table
  - _Requirements: 6.1, 6.2, 6.6_

- [x] 4.2 Create file storage services
  - Create infrastructure/filestorage/IFileStorageService interface
  - Create infrastructure/filestorage/S3FileStorageService for production
  - Create infrastructure/filestorage/LocalFileStorageService for development
  - Add profile-based service selection and configuration (similar to ingestion module)
  - _Requirements: 6.3, 6.4, 6.5_

- [ ] 4.3 Create external services
  - Create infrastructure/external/ExchangeRateProvider interface and implementation
  - Add caching for exchange rates to improve performance
  - Implement error handling for unavailable rates
  - Add configuration for exchange rate data sources
  - _Requirements: 2.1, 2.5_

- [x] 4.4 Create configuration and async processing
  - Create infrastructure/config/RiskCalculationConfiguration
  - Set up async executor configuration for parallel processing
  - Configure thread pool sizes and queue management
  - Add monitoring and metrics for thread pool usage
  - _Requirements: 9.1, 9.4_

- [ ] 5. Implement presentation layer
- [x] 5.1 Create health and monitoring endpoints







  - Implement health check controller for risk calculation service
  - Add metrics endpoints for processing statistics
  - Create status query endpoints for batch summaries
  - _Requirements: 9.5_

- [x] 6. Add comprehensive error handling and logging





- [x] 6.1 Implement retry mechanisms


  - Add exponential backoff for file download failures in FileProcessingService
  - Configure retry policies with maximum attempt limits (3 retries)
  - Add structured logging for retry attempts
  - _Requirements: 8.1_

- [x] 6.2 Add transaction rollback and error status handling


  - Ensure proper transaction boundaries with @Transactional
  - Implement error status updates for failed calculations
  - Add error message persistence and retrieval
  - _Requirements: 8.3, 8.4_

- [ ] 7. Configure integration with existing modules
- [ ] 7.1 Set up event bus integration
  - Configure integration with regtech-core event bus
  - Add event serialization and deserialization
  - Set up inbox/outbox pattern for reliable event delivery
  - _Requirements: 1.1, 7.1_

- [ ] 7.2 Add module to main application
  - Update regtech-app POM to include risk-calculation module dependencies
  - Configure Spring Boot auto-configuration
  - Add profile-specific configurations
  - _Requirements: 6.4_

- [ ] 8. Add performance optimizations and monitoring
- [ ] 8.1 Verify streaming JSON parsing implementation
  - Verify streaming parser for large exposure files is working correctly
  - Test memory usage during file processing
  - Add memory usage monitoring and alerts
  - _Requirements: 9.2_

- [ ] 8.2 Add performance monitoring and metrics
  - Implement processing time tracking
  - Add throughput metrics and logging
  - Create performance dashboards and alerts
  - _Requirements: 9.3, 9.5_

- [ ] 9. Add comprehensive testing
- [ ] 9.1 Add unit tests for domain layer
  - Write unit tests for value objects (AmountEur, ExchangeRate, etc.)
  - Write unit tests for domain entities (BatchSummary, CalculatedExposure)
  - Write unit tests for classifiers (GeographicClassifier, SectorClassifier)
  - Write unit tests for aggregation logic (HerfindahlIndex, ConcentrationCalculator)
  - _Requirements: All domain requirements_

- [ ] 9.2 Add unit tests for application layer
  - Write unit tests for command handlers
  - Write unit tests for application services
  - Write unit tests for event listeners
  - _Requirements: All application requirements_

- [ ] 9.3 Add integration tests
  - Write integration tests for database repositories
  - Write integration tests for file storage services
  - Write integration tests for event flow (BatchIngestedEvent → BatchCalculationCompletedEvent)
  - _Requirements: 1.1, 6.1, 6.3, 7.1_

- [ ] 10. Final checkpoint - Complete implementation
  - Ensure all tests pass, ask the user if questions arise.

