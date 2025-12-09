# Implementation Plan

- [x] 1. Set up project structure and domain foundations





  - Verify Maven multi-module structure (domain, application, infrastructure, presentation)
  - Review existing directory structure and layer dependencies
  - Update domain model to support bounded contexts
  - _Requirements: 1.1, 1.2_

- [x] 2. Implement Exposure Recording bounded context





- [x] 2.1 Create Exposure Recording domain model


  - Create domain/exposure/ExposureRecording aggregate root (immutable)
  - Create domain/exposure/InstrumentId value object
  - Create domain/exposure/InstrumentType enum (LOAN, BOND, DERIVATIVE, GUARANTEE, etc.)
  - Create domain/exposure/CounterpartyRef value object
  - Create domain/exposure/MonetaryAmount value object
  - Create domain/exposure/ExposureClassification value object
  - Create domain/exposure/BalanceSheetType enum
  - _Requirements: 1.1, 1.2_

- [ ]* 2.2 Write property test for Exposure Recording
  - **Property 1: Instrument Type Support**
  - **Validates: Requirements 1.2**

- [x] 2.3 Create shared domain value objects


  - Create domain/shared/ExposureId value object
  - Create domain/shared/BankInfo value object
  - Update existing BatchId if needed
  - _Requirements: 1.1_

- [x] 3. Implement Valuation Engine bounded context





- [x] 3.1 Create Valuation Engine domain model


  - Create domain/valuation/ExposureValuation aggregate root
  - Create domain/valuation/EurAmount value object with validation
  - Create domain/valuation/ExchangeRate value object with identity() factory
  - Create domain/valuation/ExchangeRateProvider interface
  - Add ExposureValuation.convert() factory method
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ]* 3.2 Write property test for currency conversion
  - **Property 4: Currency Conversion Correctness**
  - **Validates: Requirements 2.1**

- [ ]* 3.3 Write property test for original amount preservation
  - **Property 5: Original Amount Preservation**
  - **Validates: Requirements 2.3**

- [x] 4. Implement Credit Protection bounded context






- [x] 4.1 Create Credit Protection domain model

  - Create domain/protection/ProtectedExposure aggregate root
  - Create domain/protection/Mitigation entity with EUR conversion
  - Create domain/protection/MitigationType enum
  - Create domain/protection/RawMitigationData value object
  - Add ProtectedExposure.calculate() factory method
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ]* 4.2 Write property test for net exposure calculation
  - **Property 6: Net Exposure Calculation**
  - **Validates: Requirements 3.3, 3.4**

- [ ]* 4.3 Write property test for mitigation currency conversion
  - **Property 7: Mitigation Currency Conversion**
  - **Validates: Requirements 3.2**

- [x] 5. Implement Classification Service bounded context




- [x] 5.1 Create Classification domain model

  - Create domain/classification/ExposureClassifier domain service
  - Create domain/classification/GeographicRegion enum (ITALY, EU_OTHER, NON_EUROPEAN)
  - Create domain/classification/EconomicSector enum (RETAIL_MORTGAGE, SOVEREIGN, CORPORATE, BANKING, OTHER)
  - Create domain/classification/ClassifiedExposure value object
  - Implement classifyRegion() with EU_COUNTRIES set
  - Implement classifySector() with pattern matching
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ]* 5.2 Write property test for geographic classification
  - **Property 8: Geographic Classification Completeness**
  - **Validates: Requirements 4.1**

- [ ]* 5.3 Write property test for EU country classification
  - **Property 9: EU Country Classification**
  - **Validates: Requirements 4.3**

- [ ]* 5.4 Write property test for non-EU classification
  - **Property 10: Non-EU Country Classification**
  - **Validates: Requirements 4.4**

- [ ]* 5.5 Write property test for sector classification
  - **Property 11: Sector Classification Completeness**
  - **Validates: Requirements 5.1**

- [ ]* 5.6 Write property tests for sector pattern matching
  - **Property 12: Mortgage Pattern Matching**
  - **Property 13: Sovereign Pattern Matching**
  - **Property 14: Banking Pattern Matching**
  - **Property 15: Corporate Pattern Matching**
  - **Validates: Requirements 5.2, 5.3, 5.4, 5.5**

- [x] 6. Implement Portfolio Analysis bounded context






- [x] 6.1 Create Portfolio Analysis domain model

  - Create domain/analysis/PortfolioAnalysis aggregate root
  - Create domain/analysis/Breakdown value object with from() factory
  - Create domain/analysis/Share value object with calculate() factory
  - Create domain/analysis/HHI value object with calculate() factory
  - Create domain/analysis/ConcentrationLevel enum (LOW, MODERATE, HIGH)
  - Add PortfolioAnalysis.analyze() factory method
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [ ]* 6.2 Write property test for portfolio total
  - **Property 16: Portfolio Total Calculation**
  - **Validates: Requirements 6.1**

- [ ]* 6.3 Write property tests for geographic breakdown
  - **Property 17: Geographic Breakdown Sum**
  - **Property 18: Geographic Percentage Sum**
  - **Validates: Requirements 6.2**

- [ ]* 6.4 Write property tests for sector breakdown
  - **Property 19: Sector Breakdown Sum**
  - **Property 20: Sector Percentage Sum**
  - **Validates: Requirements 6.3**

- [ ]* 6.5 Write property tests for HHI calculation
  - **Property 21: HHI Formula Correctness**
  - **Property 22: HHI Range Constraint**
  - **Validates: Requirements 6.4**

- [x] 7. Implement application layer orchestration








- [x] 7.1 Create ingestion service

  - Create application/ingestion/RiskReportIngestionService
  - Create application/ingestion/RiskReportMapper (DTO → Domain)
  - Create application/ingestion/IngestedRiskReport value object
  - Add validateRawReport() and validateIngestedReport() methods
  - _Requirements: 1.1, 1.3, 1.4, 1.5, 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ]* 7.2 Write property test for validationapplication
  - **Property 2: Exposure ID Uniqueness Detection**
  - **Property 3: Mitigation Referential Integrity**
  - **Validates: Requirements 1.3, 1.4**


- [x] 7.3 Create risk calculation service


  - Create application/calculation/RiskCalculationService
  - Implement orchestration across all bounded contexts
  - Add convertMitigations() helper method
  - Coordinate: Valuation → Protection → Classification → Analysis
  - _Requirements: 2.1, 3.1, 4.1, 5.1, 6.1_



- [x] 7.4 Create result value object

  - Create application/calculation/RiskCalculationResult
  - Include batchId, bankInfo, totalExposures, analysis, ingestedAt
  - _Requirements: 6.1_

- [x] 8. Implement event-driven integration






- [x] 8.1 Create event listener

  - Create application/integration/BatchIngestedEventListener
  - Add @EventListener and @Async annotations
  - Implement handleBatchIngested() method
  - Download report from S3 using file URI
  - Call RiskReportIngestionService.ingestAndCalculate()
  - Handle exceptions and publish failure events
  - _Requirements: 1.1_

- [x] 8.2 Create event publisher


  - Create application/integration/RiskCalculationEventPublisher
  - Implement publishCalculationCompleted() method
  - Implement publishCalculationFailed() method
  - Create BatchCalculationCompletedEvent record
  - Create BatchCalculationFailedEvent record

  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 9. Implement infrastructure layer - Database






- [x] 9.1 Create database schema


  - Create migration V2__Create_risk_calculation_tables.sql
  - Add batches table (batch_id, bank_name, abi_code, lei_code, report_date, total_exposures, status, timestamps)
  - Add exposures table (exposure_id, batch_id, instrument_id, instrument_type, counterparty fields, amounts, currency, classification, timestamps)
  - Add mitigations table (id, exposure_id, batch_id, mitigation_type, value, currency_code, timestamps)
  - Add portfolio_analysis table (batch_id, totals, geographic breakdown, sector breakdown, HHI metrics, timestamps)
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 9.2 Create repository interfaces


  - Create domain/repositories/ExposureRepository interface
  - Create domain/repositories/MitigationRepository interface
  - Create domain/repositories/PortfolioAnalysisRepository interface
  - _Requirements: 7.3, 7.5, 8.1_




- [x] 9.3 Create JPA entities

  - Create infrastructure/database/entities/BatchEntity
  - Create infrastructure/database/entities/ExposureEntity
  - Create infrastructure/database/entities/MitigationEntity
  - Create infrastructure/database/entities/PortfolioAnalysisEntity



  - _Requirements: 7.1, 7.3, 7.5, 8.1_


- [x] 9.4 Create repository implementations

  - Create infrastructure/database/repositories/JpaExposureRepository
  - Create infrastructure/database/repositories/JpaMitigationRepository
  - Create infrastructure/database/repositories/JpaPortfolioAnalysisRepository
  - Create Spring Data JPA repository interfaces
  - Create entity mappers (Domain ↔ Entity)
  - _Requirements: 7.3, 7.5, 8.1_

- [ ]* 9.5 Write property tests for persistence
  - **Property 23: Batch Persistence Completeness**
  - **Property 24: Exposure Persistence Count**
  - **Property 25: Exposure Persistence Completeness**
  - **Property 26: Mitigation Persistence Count**
  - **Property 27: Portfolio Analysis Persistence**
  - **Property 28: Portfolio Analysis Completeness**
  - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 8.1, 8.2, 8.3, 8.4, 8.5**

- [x] 10. Implement infrastructure layer - External Services






- [x] 10.1 Create exchange rate provider

  - Create infrastructure/external/CurrencyApiExchangeRateProvider
  - Implement ExchangeRateProvider interface
  - Add WebClient configuration for currency API
  - Create infrastructure/external/CurrencyApiProperties
  - Add error handling for unavailable rates
  - _Requirements: 2.1, 2.5_


- [x] 10.2 Create configuration

  - Create infrastructure/config/RiskCalculationConfiguration
  - Configure WebClient bean for currency API
  - Set up async executor for event processing
  - Add profile-based configuration
  - _Requirements: 2.1_

- [x] 11. Implement presentation layer





- [x] 11.1 Create DTOs


  - Create presentation/dto/RiskReportDTO
  - Create presentation/dto/BankInfoDTO
  - Create presentation/dto/ExposureDTO (generic with instrument_type)
  - Create presentation/dto/CreditRiskMitigationDTO
  - _Requirements: 1.1, 1.2_

- [x] 11.2 Create health and monitoring endpoints


  - Verify existing health check controller
  - Verify existing metrics collector
  - Add batch status query endpoints if needed
  - _Requirements: 9.1_

- [ ] 12. Add comprehensive testing
- [ ]* 12.1 Add unit tests for domain layer
  - Write unit tests for value objects (ExposureId, MonetaryAmount, EurAmount, etc.)
  - Write unit tests for ExposureClassifier
  - Write unit tests for aggregate factories (ExposureValuation.convert(), ProtectedExposure.calculate(), PortfolioAnalysis.analyze())
  - Write unit tests for calculation logic (HHI.calculate(), Share.calculate())
  - _Requirements: All domain requirements_

- [ ]* 12.2 Add unit tests for application layer
  - Write unit tests for RiskReportMapper
  - Write unit tests for RiskCalculationService orchestration
  - Write unit tests for validation logic
  - _Requirements: All application requirements_

- [ ]* 12.3 Add integration tests
  - Write integration test for complete flow (DTO → PortfolioAnalysis)
  - Write integration tests for database persistence
  - Write integration test for event flow (BatchIngestedEvent → BatchCalculationCompletedEvent)
  - Write integration test for exchange rate provider
  - _Requirements: 1.1, 2.1, 6.1, 7.1, 8.1_

- [ ] 13. Final checkpoint - Complete implementation
  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Complete DDD Aggregate Refactoring (Steps 5-10)

















- [x] 14.1 Update BatchRepository interface

  - Update domain/persistence/BatchRepository.java to work with Batch aggregate
  - Add save(Batch batch) method
  - Add findById(BatchId batchId) method returning Maybe<Batch>
  - Add findByStatus(BatchStatus status) method
  - _Requirements: 7.1, 7.2_

- [x] 14.2 Refactor CalculateRiskMetricsCommandHandler


  - Add BaseUnitOfWork dependency to application/calculation/CalculateRiskMetricsCommandHandler
  - Remove RiskCalculationEventPublisher dependency
  - Use Batch.create() factory method instead of direct repository calls
  - Call batch.completeCalculation() or batch.failCalculation() for state transitions
  - Register aggregate with unitOfWork.registerEntity(batch)
  - Call unitOfWork.saveChanges() to persist events to outbox
  - Simplify error handling using Result pattern
  - _Requirements: 6.1, 7.1, 8.1_






- [x] 14.3 Remove or deprecate RiskCalculationEventPublisher
  - Delete or deprecate application/integration/RiskCalculationEventPublisher
  - Events now flow through outbox pattern instead of direct publishing



  - _Requirements: 8.1_

- [x] 14.4 Add populateEntity method to Batch aggregate


  - Add populateEntity(BatchEntity entity) method to domain/calculation/Batch
  - Implement "Tell, don't ask" principle - aggregate controls its persistence representation
  - _Requirements: 7.1, 7.2_

- [x] 14.5 Update JpaBatchRepository implementation

  - Update infrastructure/database/repositories/JpaBatchRepository to use new interface
  - Use aggregate's populateEntity method for persistence
  - Implement reconstitution from BatchEntity to Batch aggregate
  - Remove separate mapper class (aggregate handles its own persistence)

  - _Requirements: 7.1, 7.2_




- [x] 14.6 Update CalculateRiskMetricsCommandHandler tests


  - Update application/calculation/CalculateRiskMetricsCommandHandlerTest
  - Mock BaseUnitOfWork instead of RiskCalculationEventPublisher
  - Verify unitOfWork.registerEntity() is called
  - Verify unitOfWork.saveChanges() is called
  - Verify aggregate methods (create, completeCalculation, failCalculation) are called
  - Test domain event raising through aggregate behavior
  - _Requirements: 6.1, 7.1, 8.1_
