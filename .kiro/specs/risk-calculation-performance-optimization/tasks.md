# Risk Calculation Performance Optimization - Implementation Tasks

## Phase 1: Domain Layer - Exchange Rate Cache and State Management

### 1.1 Create Exchange Rate Cache Domain Components
- Create `CurrencyPair` value object in `domain/services` package
  - Implement `of(String from, String to)` factory method
  - Implement `equals()` and `hashCode()` for HashMap key usage
  - _Requirements: 1.1, 1.3_

- Create `CacheStatistics` class in `domain/services` package
  - Implement hit/miss tracking methods
  - Implement `getHitRatio()` calculation
  - _Requirements: 1.5, 7.2_

- Create `ExchangeRateCache` domain service in `domain/services` package
  - Implement `getRate(String from, String to)` with cache-first logic
  - Implement `preloadRates(Set<String> currencies, String targetCurrency)`
  - Implement `getStatistics()` method
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

### 1.2 Enhance PortfolioAnalysis with State Management
- Create `ProcessingState` enum in `domain/analysis` package
  - Define states: PENDING, IN_PROGRESS, COMPLETED, FAILED
  - _Requirements: 3.1_

- Create `ProcessingProgress` value object in `domain/analysis` package
  - Implement `initial(int totalExposures)` factory method
  - Implement `addProcessed(int count)` method
  - Implement `getPercentageComplete()` calculation
  - Implement `getEstimatedTimeRemaining()` calculation
  - _Requirements: 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- Create `ChunkMetadata` value object in `domain/analysis` package
  - Implement `of(int index, int size, Duration processingTime)` factory method
  - _Requirements: 6.1, 6.2_

- Update `PortfolioAnalysis` entity in `domain/analysis` package
  - Add `ProcessingState state` field
  - Add `ProcessingProgress progress` field
  - Add `List<ChunkMetadata> processedChunks` field
  - Implement `startProcessing(int totalExposures)` method
  - Implement `completeChunk(ChunkMetadata chunk)` method
  - Implement `complete()` method
  - Implement `fail(String reason)` method
  - Implement `canResume()` method
  - Implement `getLastProcessedChunkIndex()` method
  - _Requirements: 2.1, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

## Phase 2: Application Layer - Chunk Processing

### 2.1 Create Chunk Processor
- Create `ChunkHandler` functional interface in `application/calculation` package
  - Define `handle(List<ClassifiedExposure> classifiedExposures)` method
  - _Requirements: 2.2_

- Create `ChunkProcessor` component in `application/calculation` package
  - Inject `RiskCalculationProperties` for chunk size configuration
  - Implement `processInChunks()` method with chunk division logic
  - Implement `processChunk()` private method for single chunk processing
  - Implement `convertAndClassify()` private method using cached exchange rates
  - Add logging for chunk progress
  - _Requirements: 2.2, 2.3, 5.1, 5.2_

### 2.2 Create Persistence Strategy
- Create `PersistenceMode` enum in `application/persistence` package
  - Define modes: NONE, BATCH_SUMMARY_ONLY, FULL_DETAIL
  - _Requirements: 4.1_

- Create `ExposurePersistenceStrategy` component in `application/persistence` package
  - Inject `ExposureRepository` and `RiskCalculationProperties`
  - Implement `persistIfNeeded(List<ExposureRecording> exposures, String batchId)` method
  - Implement mode-specific persistence logic (switch statement)
  - Implement `persistAggregateData()` private method for BATCH_SUMMARY_ONLY mode
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

### 2.3 Update Command Handler
- Update `CalculateRiskMetricsCommandHandler` in `application/calculation` package
  - Remove direct `exposureRepository.saveAll()` call (line 93)
  - Create `ExchangeRateCache` instance at start of processing
  - Extract unique currencies from exposures
  - Call `cache.preloadRates()` with extracted currencies
  - Initialize `PortfolioAnalysis` with `startProcessing()`
  - Save initial PortfolioAnalysis state to database
  - Replace inline processing with `chunkProcessor.processInChunks()` call
  - Use `persistenceStrategy.persistIfNeeded()` instead of direct save
  - Update progress after each chunk via callback
  - Call `analysis.complete()` on success
  - Call `analysis.fail()` on error
  - Log cache statistics at end
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 4.5, 7.2_

## Phase 3: Infrastructure Layer - Database and Configuration

### 3.1 Update Database Schema
- Create migration `V3__Add_portfolio_analysis_state_tracking.sql` in `infrastructure/src/main/resources/db/migration`
  - Add `processing_state VARCHAR(20)` column to `portfolio_analysis` table
  - Add `total_exposures INTEGER` column
  - Add `processed_exposures INTEGER` column
  - Add `started_at TIMESTAMP` column
  - Add `last_updated_at TIMESTAMP` column
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- Create migration `V4__Create_chunk_metadata_table.sql` in `infrastructure/src/main/resources/db/migration`
  - Create `chunk_metadata` table with columns: id, portfolio_analysis_id, chunk_index, chunk_size, processed_at, processing_time_ms
  - Add foreign key constraint to `portfolio_analysis` table
  - Add unique constraint on (portfolio_analysis_id, chunk_index)
  - Create index on `portfolio_analysis_id`
  - _Requirements: 6.1, 6.2, 6.3_

### 3.2 Update Entity and Mapper
- Update `PortfolioAnalysisEntity` in `infrastructure/database/entities` package
  - Add fields for state tracking: processingState, totalExposures, processedExposures, startedAt, lastUpdatedAt
  - Add `@OneToMany` relationship to ChunkMetadataEntity
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- Create `ChunkMetadataEntity` in `infrastructure/database/entities` package
  - Define fields: id, portfolioAnalysisId, chunkIndex, chunkSize, processedAt, processingTimeMs
  - Add `@ManyToOne` relationship to PortfolioAnalysisEntity
  - _Requirements: 6.1, 6.2_

- Update `PortfolioAnalysisMapper` in `infrastructure/database/mappers` package
  - Add mapping logic for new state tracking fields
  - Add mapping logic for ChunkMetadata list
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 6.2_

### 3.3 Add Configuration Properties
- Update `RiskCalculationProperties` in `infrastructure/config` package
  - Add `batchProcessing.chunkSize` property (default: 1000)
  - Add `persistence.mode` property (default: NONE)
  - Add validation for chunk size (must be > 0)
  - _Requirements: 2.2, 4.5_

- Update `application-risk-calculation.yml` in `infrastructure/src/main/resources`
  - Add batch-processing configuration section
  - Add persistence configuration section
  - Document configuration options with comments
  - _Requirements: 2.2, 4.5_

## Phase 4: Testing

### 4.1 Unit Tests for Domain Components
- Create `ExchangeRateCacheTest` in `domain/src/test/java`
  - Test cache hit behavior
  - Test cache miss behavior
  - Test preload functionality
  - Test statistics tracking
  - _Requirements: 1.2, 1.3, 1.4, 1.5_

- Create `ProcessingProgressTest` in `domain/src/test/java`
  - Test percentage calculation
  - Test time remaining estimation
  - Test progress updates
  - _Requirements: 3.6, 3.7_

- Update `PortfolioAnalysisTest` in `domain/src/test/java`
  - Test state transitions
  - Test chunk completion tracking
  - Test resume capability check
  - _Requirements: 2.4, 2.5, 3.1_

### 4.2 Unit Tests for Application Components
- Create `ChunkProcessorTest` in `application/src/test/java`
  - Test chunk division logic
  - Test chunk processing with cache
  - Test progress updates after each chunk
  - _Requirements: 2.2, 2.3_

- Create `ExposurePersistenceStrategyTest` in `application/src/test/java`
  - Test NONE mode (no persistence)
  - Test BATCH_SUMMARY_ONLY mode
  - Test FULL_DETAIL mode
  - _Requirements: 4.2, 4.3, 4.4_

### 4.3 Integration Tests
- Update `CalculateRiskMetricsCommandHandlerIntegrationTest` in `application/src/test/java`
  - Test end-to-end processing with cache
  - Test different persistence modes
  - Verify cache hit ratio > 99% for typical batch
  - Verify state tracking in database
  - Verify chunk metadata persistence
  - Test processing with 10,000+ exposures
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 4.1, 4.2, 4.3, 4.4, 6.1, 6.2_

## Phase 5: Monitoring and Documentation

### 5.1 Add Monitoring Metrics
- Update `PerformanceMetrics` in `application/monitoring` package
  - Add cache hit ratio tracking
  - Add chunk processing time tracking
  - Add memory usage tracking
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

### 5.2 Update Documentation
- Create `PERFORMANCE_OPTIMIZATION_GUIDE.md` in `regtech-risk-calculation` root
  - Document configuration options
  - Explain persistence modes and trade-offs
  - Provide performance tuning guidelines
  - Include before/after performance metrics
  - _Requirements: All_

## Implementation Notes

### Dependencies Between Tasks
- Phase 1 must complete before Phase 2
- Phase 2 must complete before Phase 3
- Phase 3 must complete before Phase 4
- Phase 5 can be done in parallel with Phase 4

### Testing Strategy
- Unit tests for all new domain logic (Phase 4.1)
- Unit tests for application services (Phase 4.2)
- Integration tests with realistic data volumes (Phase 4.3)
- Performance validation with 1M+ exposures

### Success Criteria
- Exchange rate API calls reduced by 95%+ (from 1M to ~50 for typical batch)
- Processing time for 1M exposures < 30 minutes
- Memory usage < 2GB regardless of batch size
- Cache hit ratio > 99%
- All tests passing
- State tracking working correctly
- Resume functionality operational

### Risk Mitigation
- Implement feature flag for enabling/disabling optimization
- Maintain backward compatibility with existing code
- Add comprehensive logging for troubleshooting
- Monitor performance metrics during rollout
