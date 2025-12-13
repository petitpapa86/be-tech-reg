package com.bcbs239.regtech.riskcalculation.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity for portfolio analysis results persistence
 * Maps to the riskcalculation.portfolio_analysis table
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolio_analysis", schema = "riskcalculation", indexes = {
    @Index(name = "idx_portfolio_analysis_analyzed_at", columnList = "analyzed_at")
})
public class PortfolioAnalysisEntity {
    
    @Id
    @Column(name = "batch_id", length = 100)
    private String batchId;
    
    // Totals
    @Column(name = "total_portfolio_eur", nullable = false, precision = 20, scale = 2)
    private BigDecimal totalPortfolioEur;
    
    // Geographic breakdown
    @Column(name = "italy_amount", precision = 20, scale = 2)
    private BigDecimal italyAmount;
    
    @Column(name = "italy_percentage", precision = 5, scale = 2)
    private BigDecimal italyPercentage;
    
    @Column(name = "eu_other_amount", precision = 20, scale = 2)
    private BigDecimal euOtherAmount;
    
    @Column(name = "eu_other_percentage", precision = 5, scale = 2)
    private BigDecimal euOtherPercentage;
    
    @Column(name = "non_european_amount", precision = 20, scale = 2)
    private BigDecimal nonEuropeanAmount;
    
    @Column(name = "non_european_percentage", precision = 5, scale = 2)
    private BigDecimal nonEuropeanPercentage;
    
    // Sector breakdown
    @Column(name = "retail_mortgage_amount", precision = 20, scale = 2)
    private BigDecimal retailMortgageAmount;
    
    @Column(name = "retail_mortgage_percentage", precision = 5, scale = 2)
    private BigDecimal retailMortgagePercentage;
    
    @Column(name = "sovereign_amount", precision = 20, scale = 2)
    private BigDecimal sovereignAmount;
    
    @Column(name = "sovereign_percentage", precision = 5, scale = 2)
    private BigDecimal sovereignPercentage;
    
    @Column(name = "corporate_amount", precision = 20, scale = 2)
    private BigDecimal corporateAmount;
    
    @Column(name = "corporate_percentage", precision = 5, scale = 2)
    private BigDecimal corporatePercentage;
    
    @Column(name = "banking_amount", precision = 20, scale = 2)
    private BigDecimal bankingAmount;
    
    @Column(name = "banking_percentage", precision = 5, scale = 2)
    private BigDecimal bankingPercentage;
    
    @Column(name = "other_amount", precision = 20, scale = 2)
    private BigDecimal otherAmount;
    
    @Column(name = "other_percentage", precision = 5, scale = 2)
    private BigDecimal otherPercentage;
    
    // Concentration indices
    @Column(name = "geographic_hhi", nullable = false, precision = 6, scale = 4)
    private BigDecimal geographicHhi;
    
    @Column(name = "geographic_concentration_level", nullable = false, length = 20)
    private String geographicConcentrationLevel;
    
    @Column(name = "sector_hhi", nullable = false, precision = 6, scale = 4)
    private BigDecimal sectorHhi;
    
    @Column(name = "sector_concentration_level", nullable = false, length = 20)
    private String sectorConcentrationLevel;
    
    // Metadata
    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // State tracking fields for performance optimization
    @Column(name = "processing_state", length = 20)
    private String processingState;
    
    @Column(name = "total_exposures")
    private Integer totalExposures;
    
    @Column(name = "processed_exposures")
    private Integer processedExposures;
    
    @Column(name = "started_at")
    private Instant startedAt;
    
    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;
    
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    // Chunk metadata for detailed progress tracking
    @OneToMany(mappedBy = "portfolioAnalysis", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ChunkMetadataEntity> chunkMetadata = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        // version = 0L;
    }
}
