# Risk Calculation Performance Optimization - Design

## Overview

This design introduces performance optimizations to handle large-scale risk calculations (millions of exposures) efficiently through exchange rate caching, chunk-based processing, state management, and configurable persistence strategies.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              CalculateRiskMetricsCommandHandler             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  1. Create ExchangeRateCache (session-scoped)        │   │
│  │  2. Initialize PortfolioAnalysis (IN_PROGRESS)       │   │
│  │  3. Preload exchange rates for all currencies        │   │
│  │  4. Process exposures in chunks                      │   │
│  │  5. Update progress after each chunk                 │   │
│  │  6. Persist results (COMPLETED)                      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ ExchangeRate │   │   Chunk      │   │  Persistence │
│    Cache     │   │  Processor   │   │   Strategy   │
└──────────────┘   └──────────────┘   └──────────────┘
```

## Core Components

### 1. ExchangeRateCache (Domain Service)

A session-scoped cache that stores exchange rates for the duration of batch processing.

```java
package com.bcbs239.regtech.riskcalculation.domain.services;

public class ExchangeRateCache {
    private final Map<CurrencyPair, ExchangeRate> cache;
    private final ExchangeRateProvider provider;
    private final CacheStatistics statistics;
    
    /**
     * Gets exchange rate, checking cache first
     */
    public ExchangeRate getRate(String fromCurrency, String toCurrency) {
        CurrencyPair pair = CurrencyPair.of(fromCurrency, toCurrency);
        
        // Check cache first
        if (cache.containsKey(pair)) {
            statistics.recordHit();
            return cache.get(pair);
        }
        
        // Cache miss - fetch from provider
        statistics.recordMiss();
        ExchangeRate rate = provider.getRate(fromCurrency, toCurrency);
        cache.put(pair, rate);
        return rate;
    }
    
    /**
     * Preloads rates for all currencies in the batch
     */
    public void preloadRates(Set<String> currencies, String targetCurrency) {
        for (String currency : currencies) {
            if (!currency.equals(targetCurrency)) {
                getRate(currency, targetCurrency);
            }
        }
    }
    
    public CacheStatistics getStatistics() {
        return statistics;
    }
}

// Value objects
public record CurrencyPair(String from, String to) {
    public static CurrencyPair of(String from, String to) {
        return new CurrencyPair(from, to);
    }
}

public class CacheStatistics {
    private int hits = 0;
    private int misses = 0;
    
    public void recordHit() { hits++; }
    public void recordMiss() { misses++; }
    
    public double getHitRatio() {
        int total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
}
```

### 2. Enhanced PortfolioAnalysis (Domain Entity)

Extended with state management and progress tracking.

```java
package com.bcbs239.regtech.riskcalculation.domain.analysis;

public class PortfolioAnalysis {
    // Existing fields
    private final String batchId;
    private final HHI geographicHHI;
    private final HHI sectorHHI;
    private final Breakdown geographicBreakdown;
    private final Breakdown sectorBreakdown;
    
    // NEW: State management fields
    private ProcessingState state;
    private ProcessingProgress progress;
    private List<ChunkMetadata> processedChunks;
    
    /**
     * Starts processing - transitions to IN_PROGRESS
     */
    public void startProcessing(int totalExposures) {
        this.state = ProcessingState.IN_PROGRESS;
        this.progress = ProcessingProgress.initial(totalExposures);
        this.processedChunks = new ArrayList<>();
    }
    
    /**
     * Records completion of a chunk
     */
    public void completeChunk(ChunkMetadata chunk) {
        this.processedChunks.add(chunk);
        this.progress = progress.addProcessed(chunk.size());
    }
    
    /**
     * Marks processing as complete
     */
    public void complete() {
        this.state = ProcessingState.COMPLETED;
    }
    
    /**
     * Marks processing as failed
     */
    public void fail(String reason) {
        this.state = ProcessingState.FAILED;
    }
    
    /**
     * Checks if processing can be resumed
     */
    public boolean canResume() {
        return state == ProcessingState.IN_PROGRESS && !processedChunks.isEmpty();
    }
    
    /**
     * Gets the index of the last processed chunk
     */
    public int getLastProcessedChunkIndex() {
        return processedChunks.isEmpty() ? -1 : 
            processedChunks.get(processedChunks.size() - 1).index();
    }
}

// Supporting value objects
public enum ProcessingState {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

public record ProcessingProgress(
    int totalExposures,
    int processedExposures,
    Instant startedAt,
    Instant lastUpdateAt
) {
    public static ProcessingProgress initial(int totalExposures) {
        Instant now = Instant.now();
        return new ProcessingProgress(totalExposures, 0, now, now);
    }
    
    public ProcessingProgress addProcessed(int count) {
        return new ProcessingProgress(
            totalExposures,
            processedExposures + count,
            startedAt,
            Instant.now()
        );
    }
    
    public double getPercentageComplete() {
        return totalExposures == 0 ? 0.0 : 
            (double) processedExposures / totalExposures * 100.0;
    }
    
    public Duration getEstimatedTimeRemaining() {
        if (processedExposures == 0) return Duration.ZERO;
        
        Duration elapsed = Duration.between(startedAt, lastUpdateAt);
        long remainingExposures = totalExposures - processedExposures;
        double rate = (double) processedExposures / elapsed.toSeconds();
        
        return Duration.ofSeconds((long) (remainingExposures / rate));
    }
}

public record ChunkMetadata(
    int index,
    int size,
    Instant processedAt,
    Duration processingTime
) {
    public static ChunkMetadata of(int index, int size, Duration processingTime) {
        return new ChunkMetadata(index, size, Instant.now(), processingTime);
    }
}
```

### 3. ChunkProcessor (Application Service)

Handles chunk-based processing logic.

```java
package com.bcbs239.regtech.riskcalculation.application.calculation;

@Component
public class ChunkProcessor {
    private final RiskCalculationProperties properties;
    
    /**
     * Processes exposures in chunks
     */
    public void processInChunks(
        List<ExposureRecording> exposures,
        PortfolioAnalysis analysis,
        ExchangeRateCache cache,
        ExposureClassifier classifier,
        ChunkHandler handler
    ) {
        int chunkSize = properties.getChunkSize(); // default 1000
        int totalChunks = (int) Math.ceil((double) exposures.size() / chunkSize);
        
        for (int i = 0; i < totalChunks; i++) {
            int startIdx = i * chunkSize;
            int endIdx = Math.min(startIdx + chunkSize, exposures.size());
            List<ExposureRecording> chunk = exposures.subList(startIdx, endIdx);
            
            Instant chunkStart = Instant.now();
            
            // Process chunk
            List<ClassifiedExposure> classified = processChunk(
                chunk, cache, classifier
            );
            
            // Let handler do something with classified exposures
            handler.handle(classified);
            
            // Record chunk completion
            Duration processingTime = Duration.between(chunkStart, Instant.now());
            ChunkMetadata metadata = ChunkMetadata.of(i, chunk.size(), processingTime);
            analysis.completeChunk(metadata);
            
            log.info("Processed chunk {}/{}: {} exposures in {}ms",
                i + 1, totalChunks, chunk.size(), processingTime.toMillis());
        }
    }
    
    private List<ClassifiedExposure> processChunk(
        List<ExposureRecording> chunk,
        ExchangeRateCache cache,
        ExposureClassifier classifier
    ) {
        return chunk.stream()
            .map(exposure -> convertAndClassify(exposure, cache, classifier))
            .collect(Collectors.toList());
    }
    
    private ClassifiedExposure convertAndClassify(
        ExposureRecording exposure,
        ExchangeRateCache cache,
        ExposureClassifier classifier
    ) {
        // Convert to EUR using cached rates
        EurAmount eurAmount;
        String currency = exposure.exposureAmount().currencyCode();
        
        if ("EUR".equals(currency)) {
            eurAmount = EurAmount.of(exposure.exposureAmount().amount());
        } else {
            ExchangeRate rate = cache.getRate(currency, "EUR");
            BigDecimal eurValue = exposure.exposureAmount().amount()
                .multiply(rate.rate());
            eurAmount = EurAmount.of(eurValue);
        }
        
        // Classify
        GeographicRegion region = classifier.classifyRegion(
            exposure.classification().countryCode()
        );
        EconomicSector sector = classifier.classifySector(
            exposure.classification().productType()
        );
        
        return ClassifiedExposure.of(
            exposure.id(),
            eurAmount,
            region,
            sector
        );
    }
}

@FunctionalInterface
public interface ChunkHandler {
    void handle(List<ClassifiedExposure> classifiedExposures);
}
```

### 4. Persistence Strategy

Configurable strategy for ExposureRecording persistence.

```java
package com.bcbs239.regtech.riskcalculation.application.persistence;

public enum PersistenceMode {
    NONE,                // No ExposureRecording persistence
    BATCH_SUMMARY_ONLY,  // Only aggregate data
    FULL_DETAIL          // All ExposureRecording instances
}

@Component
public class ExposurePersistenceStrategy {
    private final ExposureRepository exposureRepository;
    private final RiskCalculationProperties properties;
    
    public void persistIfNeeded(
        List<ExposureRecording> exposures,
        String batchId
    ) {
        PersistenceMode mode = properties.getPersistenceMode();
        
        switch (mode) {
            case NONE:
                log.debug("Persistence mode NONE - skipping exposure persistence");
                break;
                
            case BATCH_SUMMARY_ONLY:
                log.debug("Persistence mode BATCH_SUMMARY_ONLY - saving aggregates only");
                persistAggregateData(exposures, batchId);
                break;
                
            case FULL_DETAIL:
                log.debug("Persistence mode FULL_DETAIL - saving all exposures");
                exposureRepository.saveAll(exposures, batchId);
                break;
        }
    }
    
    private void persistAggregateData(
        List<ExposureRecording> exposures,
        String batchId
    ) {
        // Save only summary statistics
        int count = exposures.size();
        BigDecimal totalAmount = exposures.stream()
            .map(e -> e.exposureAmount().amount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        // Persist to a summary table (not full exposure records)
        // Implementation depends on your needs
    }
}
```

### 5. Updated Command Handler Flow

```java
@Transactional
public Result<Void> handle(CalculateRiskMetricsCommand command) {
    String batchId = command.getBatchId();
    
    try {
        // Step 1: Download and parse exposures (unchanged)
        List<ExposureRecording> exposures = downloadAndParse(command);
        
        // Step 2: Create exchange rate cache
        ExchangeRateCache cache = new ExchangeRateCache(exchangeRateProvider);
        
        // Step 3: Preload exchange rates
        Set<String> currencies = exposures.stream()
            .map(e -> e.exposureAmount().currencyCode())
            .collect(Collectors.toSet());
        cache.preloadRates(currencies, "EUR");
        
        log.info("Preloaded {} exchange rates, cache hit ratio will be tracked",
            currencies.size());
        
        // Step 4: Initialize portfolio analysis with state tracking
        PortfolioAnalysis analysis = PortfolioAnalysis.createPending(batchId);
        analysis.startProcessing(exposures.size());
        portfolioAnalysisRepository.save(analysis); // Save initial state
        
        // Step 5: Process in chunks
        List<ClassifiedExposure> allClassified = new ArrayList<>();
        ExposureClassifier classifier = new ExposureClassifier();
        
        chunkProcessor.processInChunks(
            exposures,
            analysis,
            cache,
            classifier,
            classifiedChunk -> {
                allClassified.addAll(classifiedChunk);
                // Persist progress after each chunk
                portfolioAnalysisRepository.save(analysis);
            }
        );
        
        // Step 6: Optionally persist exposures based on configuration
        persistenceStrategy.persistIfNeeded(exposures, batchId);
        
        // Step 7: Complete analysis with all classified exposures
        analysis = PortfolioAnalysis.analyze(batchId, allClassified);
        analysis.complete();
        portfolioAnalysisRepository.save(analysis);
        
        // Step 8: Log cache statistics
        CacheStatistics stats = cache.getStatistics();
        log.info("Exchange rate cache hit ratio: {:.2f}%",
            stats.getHitRatio() * 100);
        
        // Step 9: Publish success event
        eventPublisher.publishBatchCalculationCompleted(
            batchId, command.getBankId(), allClassified.size()
        );
        
        return Result.success();
        
    } catch (Exception e) {
        log.error("Risk calculation failed for batch: {}", batchId, e);
        // Mark analysis as failed if it exists
        portfolioAnalysisRepository.findByBatchId(batchId)
            .ifPresent(analysis -> {
                analysis.fail(e.getMessage());
                portfolioAnalysisRepository.save(analysis);
            });
        
        eventPublisher.publishBatchCalculationFailed(
            batchId, command.getBankId(), e.getMessage()
        );
        
        return Result.failure(ErrorDetail.of("CALCULATION_FAILED",
            ErrorType.SYSTEM_ERROR, e.getMessage(), "calculation.failed"));
    }
}
```

## Database Schema Changes

### Enhanced PortfolioAnalysis Table

```sql
ALTER TABLE portfolio_analysis 
ADD COLUMN processing_state VARCHAR(20),
ADD COLUMN total_exposures INTEGER,
ADD COLUMN processed_exposures INTEGER,
ADD COLUMN started_at TIMESTAMP,
ADD COLUMN last_updated_at TIMESTAMP;
```

### New ChunkMetadata Table

```sql
CREATE TABLE chunk_metadata (
    id UUID PRIMARY KEY,
    portfolio_analysis_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    processing_time_ms BIGINT NOT NULL,
    FOREIGN KEY (portfolio_analysis_id) 
        REFERENCES portfolio_analysis(id) ON DELETE CASCADE,
    UNIQUE (portfolio_analysis_id, chunk_index)
);

CREATE INDEX idx_chunk_metadata_portfolio 
    ON chunk_metadata(portfolio_analysis_id);
```

## Configuration

```yaml
risk-calculation:
  batch-processing:
    chunk-size: 1000  # Exposures per chunk
    
  persistence:
    mode: NONE  # NONE, BATCH_SUMMARY_ONLY, FULL_DETAIL
    
  exchange-rate-cache:
    enabled: true
    preload-enabled: true
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Exchange Rate Cache Consistency

*For any* batch processing session and any currency pair, if the exchange rate is fetched once, all subsequent requests for that currency pair within the same session should return the same rate without calling the external API.

**Validates: Requirements 1.2, 1.3, 1.4**

### Property 2: Chunk Processing Completeness

*For any* list of exposures divided into chunks, the total number of processed exposures across all chunks should equal the original list size.

**Validates: Requirements 2.2, 2.3**

### Property 3: Progress Monotonicity

*For any* PortfolioAnalysis during processing, the processed exposure count should never decrease and should eventually equal the total exposure count.

**Validates: Requirements 3.2, 3.3**

### Property 4: State Transition Validity

*For any* PortfolioAnalysis, state transitions should follow the valid sequence: PENDING → IN_PROGRESS → (COMPLETED | FAILED), and no other transitions should be possible.

**Validates: Requirements 3.1**

### Property 5: Persistence Mode Compliance

*For any* batch processing with persistence mode NONE, zero ExposureRecording instances should be saved to the database.

**Validates: Requirements 4.2**

### Property 6: Cache Hit Ratio Improvement

*For any* batch with N unique currencies, the number of exchange rate API calls should not exceed N, regardless of the total number of exposures.

**Validates: Requirements 1.1, 1.3**

### Property 7: Chunk Metadata Completeness

*For any* completed batch processing, the number of ChunkMetadata records should equal the total number of chunks processed.

**Validates: Requirements 6.1, 6.2**

### Property 8: Memory Efficiency

*For any* chunk processing operation, after the chunk completes, references to processed ExposureRecording objects in that chunk should be eligible for garbage collection.

**Validates: Requirements 5.2**

## Testing Strategy

### Unit Tests
- ExchangeRateCache hit/miss behavior
- PortfolioAnalysis state transitions
- ChunkProcessor chunk division logic
- PersistenceStrategy mode selection
- ProcessingProgress percentage calculations

### Property-Based Tests
We will use **JUnit QuickCheck** for property-based testing in Java.

Each property-based test will:
- Run a minimum of 100 iterations
- Be tagged with the format: `**Feature: risk-calculation-performance-optimization, Property {number}: {property_text}**`
- Reference the corresponding correctness property from the design document

Example:
```java
@Property(trials = 100)
// **Feature: risk-calculation-performance-optimization, Property 1: Exchange Rate Cache Consistency**
public void exchangeRateCacheConsistency(
    @ForAll List<@From(CurrencyGenerator.class) String> currencies
) {
    // Test implementation
}
```

### Integration Tests
- End-to-end batch processing with different persistence modes
- Resume functionality after simulated failure
- Cache statistics accuracy
- Database state after chunk processing

## Performance Expectations

### Before Optimization
- 1M exposures with 50 unique currencies
- Exchange rate API calls: 1,000,000
- Processing time: ~2+ hours (with API rate limits)
- Memory usage: Unpredictable (all exposures in memory)

### After Optimization
- 1M exposures with 50 unique currencies
- Exchange rate API calls: 50 (99.995% reduction!)
- Processing time: <30 minutes
- Memory usage: <2GB (controlled via chunking)
- Cache hit ratio: >99%

## Error Handling

### Transient Errors
- Exchange rate API failures: Retry with exponential backoff
- Database connection issues: Retry chunk processing
- Memory pressure: Reduce chunk size dynamically

### Permanent Errors
- Mark PortfolioAnalysis as FAILED
- Preserve all successfully processed chunks
- Log detailed error information
- Notify operators via event

## Monitoring

### Metrics to Expose
- Processing throughput (exposures/second)
- Exchange rate cache hit ratio
- Average chunk processing time
- Memory utilization
- Current processing state and progress percentage

### Health Checks
- Exchange rate API availability
- Database connection health
- Memory threshold compliance
