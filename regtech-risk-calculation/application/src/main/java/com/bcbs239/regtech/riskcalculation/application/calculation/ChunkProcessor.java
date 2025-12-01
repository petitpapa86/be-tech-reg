package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.riskcalculation.domain.analysis.ChunkMetadata;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.services.ExchangeRateCache;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExchangeRate;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component responsible for processing exposures in chunks to optimize memory usage
 * and provide progress tracking for large batch operations.
 */
@Component
public class ChunkProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ChunkProcessor.class);
    
    private final int chunkSize;
    
    public ChunkProcessor() {
        this.chunkSize = 1000; // Default chunk size, will be configurable later
    }
    
    /**
     * Processes exposures in chunks, providing progress updates and memory management.
     * 
     * @param exposures the list of exposures to process
     * @param analysis the portfolio analysis to update with progress
     * @param cache the exchange rate cache to use for currency conversions
     * @param classifier the exposure classifier for geographic and sector classification
     * @param handler the handler to process each chunk of classified exposures
     */
    public void processInChunks(
        List<ExposureRecording> exposures,
        PortfolioAnalysis analysis,
        ExchangeRateCache cache,
        ExposureClassifier classifier,
        ChunkHandler handler
    ) {
        int chunkSizeToUse = this.chunkSize;
        int totalChunks = (int) Math.ceil((double) exposures.size() / chunkSizeToUse);
        
        log.info("Processing {} exposures in {} chunks of size {}", 
            exposures.size(), totalChunks, chunkSizeToUse);
        
        for (int i = 0; i < totalChunks; i++) {
            int startIdx = i * chunkSizeToUse;
            int endIdx = Math.min(startIdx + chunkSizeToUse, exposures.size());
            List<ExposureRecording> chunk = exposures.subList(startIdx, endIdx);
            
            Instant chunkStart = Instant.now();
            
            // Process chunk
            List<ClassifiedExposure> classifiedChunk = processChunk(chunk, cache, classifier);
            
            // Let handler process the classified exposures
            handler.handle(classifiedChunk);
            
            // Record chunk completion
            Duration processingTime = Duration.between(chunkStart, Instant.now());
            ChunkMetadata metadata = ChunkMetadata.of(i, chunk.size(), processingTime);
            analysis.completeChunk(metadata);
            
            // Log progress
            double progressPercent = analysis.getProgress() != null ? 
                analysis.getProgress().getPercentageComplete() : 0.0;
            
            log.info("Completed chunk {}/{}: {} exposures in {}ms ({}% complete, {:.1f} exp/sec)",
                i + 1, totalChunks, chunk.size(), processingTime.toMillis(),
                String.format("%.1f", progressPercent), metadata.getProcessingRate());
            
            // Suggest garbage collection after each chunk to manage memory
            if (i % 10 == 0 && i > 0) { // Every 10 chunks
                System.gc();
                log.debug("Suggested garbage collection after chunk {}", i + 1);
            }
        }
        
        log.info("Chunk processing completed: {} chunks, {} total exposures", 
            totalChunks, exposures.size());
    }
    
    /**
     * Processes a single chunk of exposures, converting currencies and classifying.
     * 
     * @param chunk the chunk of exposures to process
     * @param cache the exchange rate cache
     * @param classifier the exposure classifier
     * @return list of classified exposures
     */
    private List<ClassifiedExposure> processChunk(
        List<ExposureRecording> chunk,
        ExchangeRateCache cache,
        ExposureClassifier classifier
    ) {
        return chunk.stream()
            .map(exposure -> convertAndClassify(exposure, cache, classifier))
            .collect(Collectors.toList());
    }
    
    /**
     * Converts an exposure to EUR and classifies it by geography and sector.
     * 
     * @param exposure the exposure to convert and classify
     * @param cache the exchange rate cache
     * @param classifier the exposure classifier
     * @return classified exposure in EUR
     */
    private ClassifiedExposure convertAndClassify(
        ExposureRecording exposure,
        ExchangeRateCache cache,
        ExposureClassifier classifier
    ) {
        // Get exchange rate from cache
        String fromCurrency = exposure.getAmount().currency();
        ExchangeRate rate = cache.getRate(fromCurrency, "EUR");
        
        // Convert to EUR
        BigDecimal amountInEur = exposure.getAmount().amount().multiply(rate.rate());
        EurAmount eurAmount = EurAmount.of(amountInEur);
        
        // Classify exposure
        GeographicRegion region = classifier.classifyGeographicRegion(exposure);
        EconomicSector sector = classifier.classifyEconomicSector(exposure);
        
        return new ClassifiedExposure(exposure, eurAmount, region, sector);
    }
}
