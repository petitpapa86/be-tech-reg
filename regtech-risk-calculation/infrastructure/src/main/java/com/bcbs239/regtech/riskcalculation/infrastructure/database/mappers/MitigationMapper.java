package com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers;

import com.bcbs239.regtech.riskcalculation.domain.protection.MitigationType;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.MitigationEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between RawMitigationData domain model and MitigationEntity
 */
@Component
public class MitigationMapper {
    
    /**
     * Convert domain model to JPA entity
     * 
     * @param exposureId the exposure this mitigation applies to
     * @param batchId the batch identifier
     * @param mitigation the domain mitigation data
     * @return mitigation entity
     */
    public MitigationEntity toEntity(ExposureId exposureId, String batchId, RawMitigationData mitigation) {
        return MitigationEntity.builder()
            .exposureId(exposureId.value())
            .batchId(batchId)
            .mitigationType(mitigation.type().name())
            .value(mitigation.value())
            .currencyCode(mitigation.currency())
            .build();
    }
    
    /**
     * Convert JPA entity to domain model
     * 
     * @param entity the mitigation entity
     * @return domain mitigation data
     */
    public RawMitigationData toDomain(MitigationEntity entity) {
        return new RawMitigationData(
            MitigationType.valueOf(entity.getMitigationType()),
            entity.getValue(),
            entity.getCurrencyCode()
        );
    }
}
