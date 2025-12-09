package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.persistence.PortfolioAnalysisRepository;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.PortfolioAnalysisEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.PortfolioAnalysisMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * JPA implementation of PortfolioAnalysisRepository
 * Adapts Spring Data JPA repository to domain repository interface
 */
@Repository
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
        PortfolioAnalysisEntity entity = mapper.toEntity(analysis);
        springDataRepository.save(entity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<PortfolioAnalysis> findByBatchId(String batchId) {
        return springDataRepository.findById(batchId)
            .map(mapper::toDomain);
    }
}
