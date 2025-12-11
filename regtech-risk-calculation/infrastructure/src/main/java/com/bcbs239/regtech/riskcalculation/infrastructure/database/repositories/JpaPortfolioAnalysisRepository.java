package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.PortfolioAnalysisEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.PortfolioAnalysisMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA implementation of PortfolioAnalysisRepository
 * Adapts Spring Data JPA repository to domain repository interface
 */
@Repository
@Slf4j
public class JpaPortfolioAnalysisRepository implements PortfolioAnalysisRepository {
    
    private final SpringDataPortfolioAnalysisRepository springDataRepository;
    private final PortfolioAnalysisMapper mapper;
    
    public JpaPortfolioAnalysisRepository(
        SpringDataPortfolioAnalysisRepository springDataRepository,
        PortfolioAnalysisMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public void save(PortfolioAnalysis analysis) {
        try {
            // 1. Find or create entity
            Optional<PortfolioAnalysisEntity> existing = springDataRepository.findById(analysis.getBatchId());
            
            PortfolioAnalysisEntity entity;
            boolean isNew = false;
            
            if (existing.isPresent()) {
                // UPDATE existing entity
                entity = existing.get();
                log.debug("Updating existing portfolio analysis for batch: {}", analysis.getBatchId());
            } else {
                // CREATE new entity
                entity = mapper.toEntity(analysis);
                entity.setVersion(0L);  // CRITICAL: Set version for new entities
                isNew = true;
                log.debug("Creating new portfolio analysis for batch: {}", analysis.getBatchId());
            }
            
            // 2. Update all fields from domain
            updateEntityFromDomain(entity, analysis);
            
            // 3. Save
            springDataRepository.save(entity);
            
            log.debug("Successfully saved portfolio analysis for batch: {} (new: {})", 
                analysis.getBatchId(), isNew);
            
        } catch (DataIntegrityViolationException e) {
            handleDuplicateKeyOrConstraintViolation(e, analysis);
            
        } catch (OptimisticLockingFailureException e) {
            handleOptimisticLockConflict(analysis);
        }
    }
    
    /**
     * Handle duplicate key (batch_id) or other constraint violations
     */
    private void handleDuplicateKeyOrConstraintViolation(
            DataIntegrityViolationException e, PortfolioAnalysis analysis) {
        
        String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        if (errorMsg.contains("batch_id") || errorMsg.contains("portfolio_analysis_pkey")) {
            log.debug("Duplicate batch_id detected for portfolio analysis: {} - This is expected in concurrent processing", 
                analysis.getBatchId());
            // Silently skip - another thread already created this analysis
            return;
        }
        
        log.error("Data integrity violation while saving portfolio analysis for batch: {}", 
            analysis.getBatchId(), e);
        throw e;
    }
    
    /**
     * Handle optimistic lock conflicts
     */
    private void handleOptimisticLockConflict(PortfolioAnalysis analysis) {
        log.warn("Optimistic lock conflict for portfolio analysis: {}", analysis.getBatchId());
        
        // Try to get the latest version
        try {
            Optional<PortfolioAnalysisEntity> latest = springDataRepository.findById(analysis.getBatchId());
            if (latest.isPresent()) {
                log.debug("Latest version exists after optimistic lock conflict: {}", 
                    analysis.getBatchId());
            }
        } catch (Exception ex) {
            log.error("Failed to fetch latest after optimistic lock conflict: {}", 
                analysis.getBatchId(), ex);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioAnalysis> findByBatchId(String batchId) {
        return springDataRepository.findById(batchId)
            .map(mapper::toDomain);
    }
    
    /**
     * Updates an existing entity with values from the domain model.
     * This method preserves the entity's version field for optimistic locking.
     * 
     * @param entity the entity to update
     * @param analysis the domain model with new values
     */
    private void updateEntityFromDomain(PortfolioAnalysisEntity entity, PortfolioAnalysis analysis) {
        entity.setBatchId(analysis.getBatchId());
        entity.setTotalPortfolioEur(analysis.getTotalPortfolio().value());
        entity.setGeographicHhi(analysis.getGeographicHHI().value());
        entity.setGeographicConcentrationLevel(analysis.getGeographicHHI().level().name());
        entity.setSectorHhi(analysis.getSectorHHI().value());
        entity.setSectorConcentrationLevel(analysis.getSectorHHI().level().name());
        entity.setAnalyzedAt(analysis.getAnalyzedAt());
        
        // Update state tracking fields if present
        if (analysis.getState() != null) {
            entity.setProcessingState(analysis.getState().name());
        }
        if (analysis.getProgress() != null) {
            entity.setTotalExposures(analysis.getProgress().totalExposures());
            entity.setProcessedExposures(analysis.getProgress().processedExposures());
        }
        if (analysis.getStartedAt() != null) {
            entity.setStartedAt(analysis.getStartedAt());
        }
        if (analysis.getLastUpdatedAt() != null) {
            entity.setLastUpdatedAt(analysis.getLastUpdatedAt());
        }
        
        // Update geographic breakdown
        var geoBreakdown = analysis.getGeographicBreakdown();
        if (geoBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion.ITALY.name())) {
            var italyShare = geoBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion.ITALY.name());
            entity.setItalyAmount(italyShare.amount().value());
            entity.setItalyPercentage(italyShare.percentage());
        }
        if (geoBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion.EU_OTHER.name())) {
            var euShare = geoBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion.EU_OTHER.name());
            entity.setEuOtherAmount(euShare.amount().value());
            entity.setEuOtherPercentage(euShare.percentage());
        }
        if (geoBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion.NON_EUROPEAN.name())) {
            var nonEuShare = geoBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.shared.enums.GeographicRegion.NON_EUROPEAN.name());
            entity.setNonEuropeanAmount(nonEuShare.amount().value());
            entity.setNonEuropeanPercentage(nonEuShare.percentage());
        }
        
        // Update sector breakdown
        var sectorBreakdown = analysis.getSectorBreakdown();
        if (sectorBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.RETAIL_MORTGAGE.name())) {
            var retailShare = sectorBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.RETAIL_MORTGAGE.name());
            entity.setRetailMortgageAmount(retailShare.amount().value());
            entity.setRetailMortgagePercentage(retailShare.percentage());
        }
        if (sectorBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.SOVEREIGN.name())) {
            var sovereignShare = sectorBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.SOVEREIGN.name());
            entity.setSovereignAmount(sovereignShare.amount().value());
            entity.setSovereignPercentage(sovereignShare.percentage());
        }
        if (sectorBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.CORPORATE.name())) {
            var corporateShare = sectorBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.CORPORATE.name());
            entity.setCorporateAmount(corporateShare.amount().value());
            entity.setCorporatePercentage(corporateShare.percentage());
        }
        if (sectorBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.BANKING.name())) {
            var bankingShare = sectorBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.BANKING.name());
            entity.setBankingAmount(bankingShare.amount().value());
            entity.setBankingPercentage(bankingShare.percentage());
        }
        if (sectorBreakdown.hasCategory(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.OTHER.name())) {
            var otherShare = sectorBreakdown.getShare(com.bcbs239.regtech.riskcalculation.domain.classification.EconomicSector.OTHER.name());
            entity.setOtherAmount(otherShare.amount().value());
            entity.setOtherPercentage(otherShare.percentage());
        }
    }
}
