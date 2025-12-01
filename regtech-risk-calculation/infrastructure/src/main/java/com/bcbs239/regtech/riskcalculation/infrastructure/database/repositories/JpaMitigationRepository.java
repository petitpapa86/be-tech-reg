package com.bcbs239.regtech.riskcalculation.infrastructure.database.repositories;

import com.bcbs239.regtech.riskcalculation.domain.persistence.MitigationRepository;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.MitigationEntity;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers.MitigationMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA implementation of MitigationRepository
 * Adapts Spring Data JPA repository to domain repository interface
 */
@Repository
public class JpaMitigationRepository implements MitigationRepository {
    
    private final SpringDataMitigationRepository springDataRepository;
    private final MitigationMapper mapper;
    
    public JpaMitigationRepository(
        SpringDataMitigationRepository springDataRepository,
        MitigationMapper mapper
    ) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional
    public void save(ExposureId exposureId, String batchId, RawMitigationData mitigation) {
        MitigationEntity entity = mapper.toEntity(exposureId, batchId, mitigation);
        springDataRepository.save(entity);
    }
    
    @Override
    @Transactional
    public void saveAll(ExposureId exposureId, String batchId, List<RawMitigationData> mitigations) {
        List<MitigationEntity> entities = mitigations.stream()
            .map(mitigation -> mapper.toEntity(exposureId, batchId, mitigation))
            .collect(Collectors.toList());
        springDataRepository.saveAll(entities);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RawMitigationData> findByExposureId(ExposureId exposureId) {
        return springDataRepository.findByExposureId(exposureId.value()).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RawMitigationData> findByBatchId(String batchId) {
        return springDataRepository.findByBatchId(batchId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
