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
            PortfolioAnalysisEntity entity = mapper.toEntity(analysis);
            springDataRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Check if this is a duplicate batch_id primary key violation
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMsg.contains("batch_id") || errorMsg.contains("portfolio_analysis_pkey")) {
                log.debug("Duplicate batch_id detected for portfolio analysis: {} - This is expected in concurrent processing", 
                    analysis.getBatchId());
                // Silently skip - another thread already created this analysis
                return;
            }
            log.error("Data integrity violation while saving portfolio analysis for batch: {}", analysis.getBatchId(), e);
            throw e;
        } catch (OptimisticLockingFailureException e) {
            log.error("Optimistic locking failure while saving portfolio analysis for batch: {}", analysis.getBatchId(), e);
            // For optimistic locking failures, we log the error but don't throw it
            // to avoid triggering event publishing rollbacks. The caller can handle retries.
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioAnalysis> findByBatchId(String batchId) {
        return springDataRepository.findById(batchId)
            .map(mapper::toDomain);
    }
}
