package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.persistence.ExposureRepository;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.ExposureEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.ExposureMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of ExposureRepository
 * Adapts Spring Data JPA repository to domain repository interface
 */
@Repository
public class JpaExposureRepository implements ExposureRepository {
    
    private final SpringDataExposureRepository springDataRepository;
    private final ExposureMapper mapper;
    
    public JpaExposureRepository(
        SpringDataExposureRepository springDataRepository,
        ExposureMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public void save(ExposureRecording exposure, String batchId) {
        ExposureEntity entity = mapper.toEntity(exposure, batchId);
        springDataRepository.save(entity);
    }
    
    @Override
    @Transactional
    public void saveAll(List<ExposureRecording> exposures, String batchId) {
        List<ExposureEntity> entities = exposures.stream()
            .map(exposure -> mapper.toEntity(exposure, batchId))
            .collect(Collectors.toList());
        springDataRepository.saveAll(entities);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ExposureRecording> findById(ExposureId id) {
        return springDataRepository.findById(id.value())
            .map(mapper::toDomain);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ExposureRecording> findByBatchId(String batchId) {
        return springDataRepository.findByBatchId(batchId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
