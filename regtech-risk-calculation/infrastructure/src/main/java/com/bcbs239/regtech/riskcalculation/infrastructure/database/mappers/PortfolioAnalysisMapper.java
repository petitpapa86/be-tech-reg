package com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers;

import com.bcbs239.regtech.riskcalculation.domain.analysis.Breakdown;
import com.bcbs239.regtech.riskcalculation.domain.analysis.HHI;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.analysis.Share;
import com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.ConcentrationLevel;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.PortfolioAnalysisEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for converting between PortfolioAnalysis domain model and PortfolioAnalysisEntity
 */
@Component("portfolioAnalysisEntityMapper")
public class PortfolioAnalysisMapper {
    
    /**
     * Convert domain model to JPA entity
     * 
     * @param analysis the domain portfolio analysis
     * @return portfolio analysis entity
     */
    public PortfolioAnalysisEntity toEntity(PortfolioAnalysis analysis) {
        PortfolioAnalysisEntity.PortfolioAnalysisEntityBuilder builder = PortfolioAnalysisEntity.builder()
            .batchId(analysis.getBatchId())
            .totalPortfolioEur(analysis.getTotalPortfolio().value())
            .geographicHhi(analysis.getGeographicHHI().value())
            .geographicConcentrationLevel(analysis.getGeographicHHI().level().name())
            .sectorHhi(analysis.getSectorHHI().value())
            .sectorConcentrationLevel(analysis.getSectorHHI().level().name())
            .analyzedAt(analysis.getAnalyzedAt());
        
        // Map state tracking fields if present
        if (analysis.getState() != null) {
            builder.processingState(analysis.getState().name());
        }
        if (analysis.getProgress() != null) {
            builder.totalExposures(analysis.getProgress().totalExposures())
                   .processedExposures(analysis.getProgress().processedExposures());
        }
        if (analysis.getStartedAt() != null) {
            builder.startedAt(analysis.getStartedAt());
        }
        if (analysis.getLastUpdatedAt() != null) {
            builder.lastUpdatedAt(analysis.getLastUpdatedAt());
        }
        
        // Map geographic breakdown
        Breakdown geoBreakdown = analysis.getGeographicBreakdown();
        if (geoBreakdown.hasCategory(GeographicRegion.ITALY.name())) {
            Share italyShare = geoBreakdown.getShare(GeographicRegion.ITALY.name());
            builder.italyAmount(italyShare.amount().value())
                   .italyPercentage(italyShare.percentage());
        }
        if (geoBreakdown.hasCategory(GeographicRegion.EU_OTHER.name())) {
            Share euShare = geoBreakdown.getShare(GeographicRegion.EU_OTHER.name());
            builder.euOtherAmount(euShare.amount().value())
                   .euOtherPercentage(euShare.percentage());
        }
        if (geoBreakdown.hasCategory(GeographicRegion.NON_EUROPEAN.name())) {
            Share nonEuShare = geoBreakdown.getShare(GeographicRegion.NON_EUROPEAN.name());
            builder.nonEuropeanAmount(nonEuShare.amount().value())
                   .nonEuropeanPercentage(nonEuShare.percentage());
        }
        
        // Map sector breakdown
        Breakdown sectorBreakdown = analysis.getSectorBreakdown();
        if (sectorBreakdown.hasCategory(EconomicSector.RETAIL_MORTGAGE.name())) {
            Share retailShare = sectorBreakdown.getShare(EconomicSector.RETAIL_MORTGAGE.name());
            builder.retailMortgageAmount(retailShare.amount().value())
                   .retailMortgagePercentage(retailShare.percentage());
        }
        if (sectorBreakdown.hasCategory(EconomicSector.SOVEREIGN.name())) {
            Share sovereignShare = sectorBreakdown.getShare(EconomicSector.SOVEREIGN.name());
            builder.sovereignAmount(sovereignShare.amount().value())
                   .sovereignPercentage(sovereignShare.percentage());
        }
        if (sectorBreakdown.hasCategory(EconomicSector.CORPORATE.name())) {
            Share corporateShare = sectorBreakdown.getShare(EconomicSector.CORPORATE.name());
            builder.corporateAmount(corporateShare.amount().value())
                   .corporatePercentage(corporateShare.percentage());
        }
        if (sectorBreakdown.hasCategory(EconomicSector.BANKING.name())) {
            Share bankingShare = sectorBreakdown.getShare(EconomicSector.BANKING.name());
            builder.bankingAmount(bankingShare.amount().value())
                   .bankingPercentage(bankingShare.percentage());
        }
        if (sectorBreakdown.hasCategory(EconomicSector.OTHER.name())) {
            Share otherShare = sectorBreakdown.getShare(EconomicSector.OTHER.name());
            builder.otherAmount(otherShare.amount().value())
                   .otherPercentage(otherShare.percentage());
        }
        
        return builder.build();
    }
    
    /**
     * Convert JPA entity to domain model
     * 
     * @param entity the portfolio analysis entity
     * @return domain portfolio analysis
     */
    public PortfolioAnalysis toDomain(PortfolioAnalysisEntity entity) {
        // Reconstruct geographic breakdown
        Map<String, Share> geoShares = new HashMap<>();
        if (entity.getItalyAmount() != null) {
            geoShares.put(
                GeographicRegion.ITALY.name(),
                new Share(EurAmount.of(entity.getItalyAmount()), entity.getItalyPercentage())
            );
        }
        if (entity.getEuOtherAmount() != null) {
            geoShares.put(
                GeographicRegion.EU_OTHER.name(),
                new Share(EurAmount.of(entity.getEuOtherAmount()), entity.getEuOtherPercentage())
            );
        }
        if (entity.getNonEuropeanAmount() != null) {
            geoShares.put(
                GeographicRegion.NON_EUROPEAN.name(),
                new Share(EurAmount.of(entity.getNonEuropeanAmount()), entity.getNonEuropeanPercentage())
            );
        }
        Breakdown geoBreakdown = new Breakdown(geoShares);
        
        // Reconstruct sector breakdown
        Map<String, Share> sectorShares = new HashMap<>();
        if (entity.getRetailMortgageAmount() != null) {
            sectorShares.put(
                EconomicSector.RETAIL_MORTGAGE.name(),
                new Share(EurAmount.of(entity.getRetailMortgageAmount()), entity.getRetailMortgagePercentage())
            );
        }
        if (entity.getSovereignAmount() != null) {
            sectorShares.put(
                EconomicSector.SOVEREIGN.name(),
                new Share(EurAmount.of(entity.getSovereignAmount()), entity.getSovereignPercentage())
            );
        }
        if (entity.getCorporateAmount() != null) {
            sectorShares.put(
                EconomicSector.CORPORATE.name(),
                new Share(EurAmount.of(entity.getCorporateAmount()), entity.getCorporatePercentage())
            );
        }
        if (entity.getBankingAmount() != null) {
            sectorShares.put(
                EconomicSector.BANKING.name(),
                new Share(EurAmount.of(entity.getBankingAmount()), entity.getBankingPercentage())
            );
        }
        if (entity.getOtherAmount() != null) {
            sectorShares.put(
                EconomicSector.OTHER.name(),
                new Share(EurAmount.of(entity.getOtherAmount()), entity.getOtherPercentage())
            );
        }
        Breakdown sectorBreakdown = new Breakdown(sectorShares);
        
        // Reconstruct HHI values
        HHI geoHHI = new HHI(
            entity.getGeographicHhi(),
            ConcentrationLevel.valueOf(entity.getGeographicConcentrationLevel())
        );
        HHI sectorHHI = new HHI(
            entity.getSectorHhi(),
            ConcentrationLevel.valueOf(entity.getSectorConcentrationLevel())
        );
        
        // Reconstitute the domain model from persisted data
        return PortfolioAnalysis.reconstitute(
            entity.getBatchId(),
            EurAmount.of(entity.getTotalPortfolioEur()),
            geoBreakdown,
            sectorBreakdown,
            geoHHI,
            sectorHHI,
            entity.getAnalyzedAt()
        );
    }
}
