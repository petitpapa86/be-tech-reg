package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Maybe;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.PortfolioAnalysisEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.PortfolioAnalysisMapper;

import lombok.extern.slf4j.Slf4j;

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
    public Result<Void> save(PortfolioAnalysis analysis) {
        try {
            // Convert domain to entity
            PortfolioAnalysisEntity entity = mapper.toEntity(analysis);

            // Persist the entity
            springDataRepository.save(entity);

            log.debug("Successfully saved portfolio analysis for batch: {}", analysis.getBatchId());
            return Result.success();
        } catch (DataIntegrityViolationException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            if (errorMsg.contains("batch_id") || errorMsg.contains("portfolio_analysis_pkey")) {
                log.debug("Duplicate batch_id detected for portfolio analysis: {} - This is expected in concurrent processing",
                        analysis.getBatchId());
                // Silently succeed - another thread already created this analysis
                return Result.success();
            }

            log.error("Data integrity violation while saving portfolio analysis for batch: {}",
                    analysis.getBatchId(), e);
            return Result.failure(ErrorDetail.of("DATA_INTEGRITY_VIOLATION", ErrorType.SYSTEM_ERROR,
                    "Failed to save portfolio analysis due to constraint violation", "portfolio.analysis.save.failed"));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Maybe<PortfolioAnalysis> findByBatchId(String batchId) {
        return springDataRepository.findById(batchId)
            .map(mapper::toDomain)
            .map(Maybe::some)
            .orElse(Maybe.none());
    }}
