package com.bcbs239.regtech.riskcalculation.presentation.mappers;

import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.HHI;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingProgress;
import com.bcbs239.regtech.riskcalculation.domain.analysis.ProcessingState;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Share;
import com.bcbs239.regtech.riskcalculation.presentation.dto.BreakdownDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ConcentrationIndicesDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.PortfolioAnalysisResponseDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingProgressDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingStateDTO;
import com.bcbs239.regtech.riskcalculation.presentation.dto.ProcessingTimestampsDTO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for converting PortfolioAnalysis domain objects to DTOs.
 * Stateless component that provides null-safe conversion logic.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Component("portfolioAnalysisDtoMapper")
public class PortfolioAnalysisMapper {
    
    /**
     * Converts a PortfolioAnalysis domain object to a PortfolioAnalysisResponseDTO.
     * 
     * @param portfolioAnalysis the domain object to convert
     * @return the converted DTO
     * @throws MappingException if the domain object is null or contains invalid data
     */
    public PortfolioAnalysisResponseDTO toResponseDTO(PortfolioAnalysis portfolioAnalysis) {
        if (portfolioAnalysis == null) {
            throw new MappingException("PortfolioAnalysis cannot be null");
        }
        
        try {
            return PortfolioAnalysisResponseDTO.builder()
                .batchId(portfolioAnalysis.getBatchId())
                .totalPortfolioEur(portfolioAnalysis.getTotalPortfolio().value())
                .processingState(toProcessingStateDTO(portfolioAnalysis.getState()))
                .processingProgress(toProcessingProgressDTO(portfolioAnalysis.getProgress()))
                .concentrationIndices(toConcentrationIndicesDTO(
                    portfolioAnalysis.getGeographicHHI(),
                    portfolioAnalysis.getSectorHHI()
                ))
                .geographicBreakdown(toBreakdownDTO(portfolioAnalysis.getGeographicBreakdown(), "GEOGRAPHIC"))
                .sectorBreakdown(toBreakdownDTO(portfolioAnalysis.getSectorBreakdown(), "SECTOR"))
                .timestamps(toProcessingTimestampsDTO(portfolioAnalysis))
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map PortfolioAnalysis to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts ProcessingState enum to ProcessingStateDTO.
     * 
     * @param state the domain enum
     * @return the DTO enum, or null if input is null
     */
    public ProcessingStateDTO toProcessingStateDTO(ProcessingState state) {
        if (state == null) {
            return null;
        }
        
        return switch (state) {
            case PENDING -> ProcessingStateDTO.PENDING;
            case IN_PROGRESS -> ProcessingStateDTO.IN_PROGRESS;
            case COMPLETED -> ProcessingStateDTO.COMPLETED;
            case FAILED -> ProcessingStateDTO.FAILED;
        };
    }
    
    /**
     * Converts ProcessingProgress value object to ProcessingProgressDTO.
     * 
     * @param progress the domain value object
     * @return the DTO, or null if input is null
     */
    public ProcessingProgressDTO toProcessingProgressDTO(ProcessingProgress progress) {
        if (progress == null) {
            return null;
        }
        
        try {
            Long estimatedTimeRemaining = null;
            if (!progress.getEstimatedTimeRemaining().isZero()) {
                estimatedTimeRemaining = progress.getEstimatedTimeRemaining().getSeconds();
            }
            
            return ProcessingProgressDTO.builder()
                .totalExposures(progress.totalExposures())
                .processedExposures(progress.processedExposures())
                .percentageComplete(progress.getPercentageComplete())
                .processingRate(progress.getProcessingRate())
                .estimatedTimeRemainingSeconds(estimatedTimeRemaining)
                .startedAt(progress.startedAt())
                .lastUpdateAt(progress.lastUpdateAt())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map ProcessingProgress to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts geographic and sector HHI values to ConcentrationIndicesDTO.
     * 
     * @param geographicHHI the geographic HHI
     * @param sectorHHI the sector HHI
     * @return the DTO
     * @throws MappingException if either HHI is null
     */
    public ConcentrationIndicesDTO toConcentrationIndicesDTO(HHI geographicHHI, HHI sectorHHI) {
        if (geographicHHI == null || sectorHHI == null) {
            throw new MappingException("HHI values cannot be null");
        }
        
        try {
            return ConcentrationIndicesDTO.builder()
                .geographicHhi(geographicHHI.value())
                .geographicRiskLevel(geographicHHI.level().name())
                .sectorHhi(sectorHHI.value())
                .sectorRiskLevel(sectorHHI.level().name())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map HHI to ConcentrationIndicesDTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a Breakdown domain object to BreakdownDTO.
     * 
     * @param breakdown the domain breakdown
     * @param type the breakdown type ("GEOGRAPHIC" or "SECTOR")
     * @return the DTO
     * @throws MappingException if breakdown is null
     */
    public BreakdownDTO toBreakdownDTO(Breakdown breakdown, String type) {
        if (breakdown == null) {
            throw new MappingException("Breakdown cannot be null");
        }
        
        try {
            Map<String, BreakdownDTO.ShareDTO> shareDTOs = breakdown.shares().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> toShareDTO(entry.getValue())
                ));
            
            return BreakdownDTO.builder()
                .type(type)
                .shares(shareDTOs)
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map Breakdown to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts a Share value object to ShareDTO.
     * 
     * @param share the domain share
     * @return the DTO
     * @throws MappingException if share is null
     */
    public BreakdownDTO.ShareDTO toShareDTO(Share share) {
        if (share == null) {
            throw new MappingException("Share cannot be null");
        }
        
        try {
            return BreakdownDTO.ShareDTO.builder()
                .amountEur(share.amount().value())
                .percentage(share.percentage())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map Share to DTO: " + e.getMessage(), e);
        }
    }
    
    /**
     * Converts PortfolioAnalysis timestamps to ProcessingTimestampsDTO.
     * 
     * @param portfolioAnalysis the domain object
     * @return the DTO
     */
    public ProcessingTimestampsDTO toProcessingTimestampsDTO(PortfolioAnalysis portfolioAnalysis) {
        if (portfolioAnalysis == null) {
            throw new MappingException("PortfolioAnalysis cannot be null");
        }
        
        try {
            return ProcessingTimestampsDTO.builder()
                .startedAt(portfolioAnalysis.getStartedAt())
                .completedAt(portfolioAnalysis.getState() == ProcessingState.COMPLETED ? 
                    portfolioAnalysis.getAnalyzedAt() : null)
                .failedAt(portfolioAnalysis.getState() == ProcessingState.FAILED ? 
                    portfolioAnalysis.getLastUpdatedAt() : null)
                .analyzedAt(portfolioAnalysis.getAnalyzedAt())
                .build();
        } catch (Exception e) {
            throw new MappingException("Failed to map timestamps to DTO: " + e.getMessage(), e);
        }
    }
}
