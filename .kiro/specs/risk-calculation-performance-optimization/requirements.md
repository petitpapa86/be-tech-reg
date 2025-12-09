# Risk Calculation Performance Optimization - Requirements

## Introduction

This specification addresses critical performance bottlenecks in the risk calculation process that prevent the system from handling large datasets (millions of exposures) efficiently. The current implementation makes redundant exchange rate API calls and persists unnecessary data, making it unsuitable for production-scale workloads.

## Glossary

- **Exchange Rate Cache**: An in-memory cache that stores currency exchange rates for the duration of a batch processing session
- **Batch Processing**: The process of handling large datasets by dividing them into smaller, manageable chunks
- **Portfolio Analysis**: The aggregate entity that tracks risk metrics and concentration indices for a batch of exposures
- **Processing State**: The current stage of batch processing (PENDING, IN_PROGRESS, COMPLETED, FAILED)
- **Chunk**: A subset of exposures processed together as a unit (default 1000 exposures)
- **ExposureRecording**: Domain object representing a single credit exposure
- **Persistence Mode**: Strategy for saving ExposureRecording data (NONE, BATCH_SUMMARY_ONLY, FULL_DETAIL)

## Requirements

### Requirement 1: Exchange Rate Caching

**User Story:** As a system administrator, I want exchange rates to be cached during batch processing, so that we minimize redundant API calls and improve processing speed.

#### Acceptance Criteria

1. WHEN a batch processing session starts, THE system SHALL create an exchange rate cache scoped to that session
2. WHEN an exchange rate is needed for a currency pair, THE system SHALL check the cache before calling the external API
3. WHEN a currency pair is not in the cache, THE system SHALL fetch the rate from the API and store it in the cache
4. WHEN the same currency pair is requested again, THE system SHALL return the cached rate without calling the API
5. WHEN batch processing completes, THE system SHALL clear the cache to prevent stale data

### Requirement 2: Batch Processing with Progress Tracking

**User Story:** As a risk analyst, I want large batches to be processed in manageable chunks with progress tracking, so that I can monitor processing and resume if interrupted.

#### Acceptance Criteria

1. WHEN processing begins, THE PortfolioAnalysis SHALL transition to IN_PROGRESS state
2. WHEN exposures are processed, THE system SHALL divide them into configurable chunks (default 1000 exposures per chunk)
3. WHEN each chunk completes, THE PortfolioAnalysis SHALL update its processed exposure count
4. WHEN processing is interrupted, THE PortfolioAnalysis SHALL retain the last successful chunk information
5. WHEN processing resumes, THE system SHALL continue from the last successfully processed chunk

### Requirement 3: Portfolio Analysis State Management

**User Story:** As a system architect, I want PortfolioAnalysis to track processing state and progress, so that the system can handle failures gracefully and provide visibility into long-running operations.

#### Acceptance Criteria

1. THE PortfolioAnalysis SHALL maintain a processing state (PENDING, IN_PROGRESS, COMPLETED, FAILED)
2. THE PortfolioAnalysis SHALL track total exposures count
3. THE PortfolioAnalysis SHALL track processed exposures count
4. THE PortfolioAnalysis SHALL record processing start timestamp
5. THE PortfolioAnalysis SHALL record last update timestamp
6. WHEN queried, THE PortfolioAnalysis SHALL calculate percentage complete
7. WHEN queried, THE PortfolioAnalysis SHALL estimate time remaining based on current processing rate

### Requirement 4: Configurable ExposureRecording Persistence

**User Story:** As a system administrator, I want to configure whether ExposureRecording instances are persisted, so that I can balance performance with data retention requirements.

#### Acceptance Criteria

1. THE system SHALL support three persistence modes: NONE, BATCH_SUMMARY_ONLY, FULL_DETAIL
2. WHEN persistence mode is NONE, THE system SHALL not save any ExposureRecording instances
3. WHEN persistence mode is BATCH_SUMMARY_ONLY, THE system SHALL save only aggregate statistics
4. WHEN persistence mode is FULL_DETAIL, THE system SHALL save all ExposureRecording instances
5. THE persistence mode SHALL be configurable via application properties

### Requirement 5: Memory Management for Large Datasets

**User Story:** As a system administrator, I want memory usage to be controlled during large batch processing, so that the system remains stable under high load.

#### Acceptance Criteria

1. WHEN processing large batches, THE system SHALL use streaming to avoid loading all exposures into memory
2. WHEN a chunk completes processing, THE system SHALL release references to processed exposures
3. WHEN memory usage exceeds a configurable threshold, THE system SHALL log a warning
4. WHEN memory pressure is detected, THE system SHALL suggest garbage collection
5. THE system SHALL process exposures in a memory-efficient manner regardless of batch size

### Requirement 6: Chunk Metadata Tracking

**User Story:** As a developer, I want metadata about each processed chunk to be recorded, so that I can analyze performance and troubleshoot issues.

#### Acceptance Criteria

1. WHEN a chunk is processed, THE system SHALL record chunk index, size, and processing time
2. WHEN a chunk completes, THE system SHALL persist chunk metadata
3. WHEN processing fails, THE system SHALL retain metadata for successfully processed chunks
4. THE chunk metadata SHALL be queryable for monitoring and debugging

### Requirement 7: Performance Monitoring

**User Story:** As a system administrator, I want to monitor processing performance metrics, so that I can identify bottlenecks and optimize configuration.

#### Acceptance Criteria

1. THE system SHALL track processing throughput (exposures per second)
2. THE system SHALL track exchange rate cache hit ratio
3. THE system SHALL track average chunk processing time
4. THE system SHALL track memory utilization during processing
5. THE system SHALL expose these metrics via monitoring endpoints

## Performance Targets

- Process 1 million exposures in under 30 minutes
- Memory usage SHALL NOT exceed 2GB for any batch size
- Exchange rate API calls SHALL be reduced by at least 95% compared to current implementation
- System SHALL support batches up to 10 million exposures

## Non-Functional Requirements

### Reliability
- Processing SHALL be resumable after system restart
- Transactional integrity SHALL be maintained for chunk processing
- Transient failures SHALL be retried automatically

### Maintainability
- Clear separation of concerns between caching, processing, and persistence
- Configurable processing parameters
- Comprehensive logging at INFO level for progress, DEBUG for details

### Security
- No sensitive data SHALL appear in logs
- Exchange rate API integration SHALL use secure connections
- Audit trail SHALL be maintained for all processing activities
