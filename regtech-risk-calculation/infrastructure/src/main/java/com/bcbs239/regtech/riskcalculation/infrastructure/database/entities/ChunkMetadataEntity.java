package com.bcbs239.regtech.riskcalculation.infrastructure.database.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA Entity for chunk processing metadata
 * Tracks individual chunk processing for performance monitoring and debugging
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chunk_metadata", schema = "riskcalculation", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_chunk_metadata_portfolio_chunk", 
            columnNames = {"portfolio_analysis_id", "chunk_index"})
    },
    indexes = {
        @Index(name = "idx_chunk_metadata_portfolio_analysis", columnList = "portfolio_analysis_id"),
        @Index(name = "idx_chunk_metadata_processing_time", columnList = "processing_time_ms")
    }
)
public class ChunkMetadataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_analysis_id", nullable = false)
    private PortfolioAnalysisEntity portfolioAnalysis;
    
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;
    
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;
    
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
    
    @Column(name = "processing_time_ms", nullable = false)
    private Long processingTimeMs;
}
