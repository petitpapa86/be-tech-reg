package com.bcbs239.regtech.riskcalculation.domain.analysis;

import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate root for Portfolio Analysis bounded context
 * Represents the complete analysis of a portfolio including:
 * - Total portfolio amount
 * - Geographic breakdown with HHI
 * - Sector breakdown with HHI
 * 
 * This is the output of the portfolio analysis process
 */
@Getter
public class PortfolioAnalysis {
    
    private final String batchId;
    private final EurAmount totalPortfolio;
    private final Breakdown geographicBreakdown;
    private final Breakdown sectorBreakdown;
    private final HHI geographicHHI;
    private final HHI sectorHHI;
    private final Instant analyzedAt;
    
    // NEW: State management fields
    private ProcessingState state;
    private ProcessingProgress progress;
    private final List<ChunkMetadata> processedChunks;
    private Instant startedAt;
    private Instant lastUpdatedAt;
    
    private PortfolioAnalysis(
        String batchId,
        EurAmount totalPortfolio,
        Breakdown geographicBreakdown,
        Breakdown sectorBreakdown,
        HHI geographicHHI,
        HHI sectorHHI,
        Instant analyzedAt
    ) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.totalPortfolio = Objects.requireNonNull(totalPortfolio, "Total portfolio cannot be null");
        this.geographicBreakdown = Objects.requireNonNull(geographicBreakdown, "Geographic breakdown cannot be null");
        this.sectorBreakdown = Objects.requireNonNull(sectorBreakdown, "Sector breakdown cannot be null");
        this.geographicHHI = Objects.requireNonNull(geographicHHI, "Geographic HHI cannot be null");
        this.sectorHHI = Objects.requireNonNull(sectorHHI, "Sector HHI cannot be null");
        this.analyzedAt = Objects.requireNonNull(analyzedAt, "Analyzed timestamp cannot be null");
        
        // Initialize state management for completed analysis
        this.state = ProcessingState.COMPLETED;
        this.progress = null; // Not applicable for completed analysis
        this.processedChunks = new ArrayList<>();
    }
    
    /**
     * Factory method to analyze a portfolio and calculate all metrics
     * 
     * @param batchId the batch identifier
     * @param exposures list of classified exposures
     * @return PortfolioAnalysis with all calculated metrics
     */
    public static PortfolioAnalysis analyze(String batchId, List<ClassifiedExposure> exposures) {
        Objects.requireNonNull(batchId, "Batch ID cannot be null");
        Objects.requireNonNull(exposures, "Exposures list cannot be null");
        
        // Calculate total portfolio amount
        BigDecimal total = exposures.stream()
            .map(e -> e.netExposure().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        EurAmount totalPortfolio = EurAmount.of(total);
        
        // Calculate geographic breakdown
        Map<GeographicRegion, BigDecimal> geoAmounts = exposures.stream()
            .collect(Collectors.groupingBy(
                ClassifiedExposure::region,
                Collectors.mapping(
                    e -> e.netExposure().value(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        Breakdown geoBreakdown = Breakdown.from(geoAmounts, total);
        HHI geoHHI = HHI.calculate(geoBreakdown);
        
        // Calculate sector breakdown
        Map<EconomicSector, BigDecimal> sectorAmounts = exposures.stream()
            .collect(Collectors.groupingBy(
                ClassifiedExposure::sector,
                Collectors.mapping(
                    e -> e.netExposure().value(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));
        
        Breakdown sectorBreakdown = Breakdown.from(sectorAmounts, total);
        HHI sectorHHI = HHI.calculate(sectorBreakdown);
        
        return new PortfolioAnalysis(
            batchId,
            totalPortfolio,
            geoBreakdown,
            sectorBreakdown,
            geoHHI,
            sectorHHI,
            Instant.now()
        );
    }
    
    /**
     * Factory method to reconstitute a PortfolioAnalysis from persisted data.
     * Used by infrastructure layer when loading from database.
     * 
     * @param batchId the batch identifier
     * @param totalPortfolio total portfolio amount
     * @param geographicBreakdown geographic breakdown
     * @param sectorBreakdown sector breakdown
     * @param geographicHHI geographic HHI
     * @param sectorHHI sector HHI
     * @param analyzedAt timestamp when analysis was performed
     * @return reconstituted PortfolioAnalysis
     */
    public static PortfolioAnalysis reconstitute(
        String batchId,
        EurAmount totalPortfolio,
        Breakdown geographicBreakdown,
        Breakdown sectorBreakdown,
        HHI geographicHHI,
        HHI sectorHHI,
        Instant analyzedAt
    ) {
        return new PortfolioAnalysis(
            batchId,
            totalPortfolio,
            geographicBreakdown,
            sectorBreakdown,
            geographicHHI,
            sectorHHI,
            analyzedAt
        );
    }
    
    // NEW: State management methods
    
    /**
     * Starts processing - transitions to IN_PROGRESS state.
     */
    public void startProcessing(int totalExposures) {
        if (state != ProcessingState.PENDING && state != null) {
            throw new IllegalStateException("Can only start processing from PENDING state, current state: " + state);
        }
        
        this.state = ProcessingState.IN_PROGRESS;
        this.progress = ProcessingProgress.initial(totalExposures);
        this.processedChunks.clear();
        this.startedAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
    }
    
    /**
     * Records completion of a chunk.
     */
    public void completeChunk(ChunkMetadata chunk) {
        if (state != ProcessingState.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete chunks during IN_PROGRESS state, current state: " + state);
        }
        
        this.processedChunks.add(chunk);
        if (progress != null) {
            this.progress = progress.addProcessed(chunk.size());
        }
        this.lastUpdatedAt = Instant.now();
    }
    
    /**
     * Marks processing as complete.
     */
    public void complete() {
        if (state != ProcessingState.IN_PROGRESS) {
            throw new IllegalStateException("Can only complete from IN_PROGRESS state, current state: " + state);
        }
        
        this.state = ProcessingState.COMPLETED;
        // Keep progress for historical tracking
    }
    
    /**
     * Marks processing as failed.
     */
    public void fail(String reason) {
        if (state == ProcessingState.COMPLETED) {
            throw new IllegalStateException("Cannot fail already completed analysis");
        }
        
        this.state = ProcessingState.FAILED;
        // Keep progress and chunks for debugging
    }
    
    /**
     * Checks if processing can be resumed.
     */
    public boolean canResume() {
        return state == ProcessingState.IN_PROGRESS && !processedChunks.isEmpty();
    }
    
    /**
     * Gets the index of the last processed chunk.
     */
    public int getLastProcessedChunkIndex() {
        if (processedChunks.isEmpty()) {
            return -1;
        }
        return processedChunks.get(processedChunks.size() - 1).index();
    }
    
    /**
     * Gets a copy of the processed chunks list.
     */
    public List<ChunkMetadata> getProcessedChunks() {
        return new ArrayList<>(processedChunks);
    }

}
