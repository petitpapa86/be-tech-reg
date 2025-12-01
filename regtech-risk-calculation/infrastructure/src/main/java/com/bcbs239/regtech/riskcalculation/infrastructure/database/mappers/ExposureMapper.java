package com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers;

import com.bcbs239.regtech.riskcalculation.domain.exposure.*;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.ExposureEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ExposureRecording domain model and ExposureEntity
 */
@Component
public class ExposureMapper {
    
    /**
     * Convert domain model to JPA entity
     * 
     * @param exposure the domain exposure recording
     * @param batchId the batch identifier
     * @return exposure entity
     */
    public ExposureEntity toEntity(ExposureRecording exposure, String batchId) {
        return ExposureEntity.builder()
            .exposureId(exposure.getId().value())
            .batchId(batchId)
            .instrumentId(exposure.getInstrumentId().value())
            .counterpartyId(exposure.getCounterparty().counterpartyId())
            .counterpartyName(exposure.getCounterparty().name())
            .counterpartyLei(exposure.getCounterparty().leiCode().orElse(null))
            .exposureAmount(exposure.getExposureAmount().amount())
            .currencyCode(exposure.getExposureAmount().currencyCode())
            .productType(exposure.getClassification().productType())
            .instrumentType(exposure.getClassification().instrumentType().name())
            .balanceSheetType(exposure.getClassification().balanceSheetType().name())
            .countryCode(exposure.getClassification().countryCode())
            .recordedAt(exposure.getRecordedAt())
            .build();
    }
    
    /**
     * Convert JPA entity to domain model
     * 
     * @param entity the exposure entity
     * @return domain exposure recording
     */
    public ExposureRecording toDomain(ExposureEntity entity) {
        return ExposureRecording.reconstitute(
            ExposureId.of(entity.getExposureId()),
            new InstrumentId(entity.getInstrumentId()),
            CounterpartyRef.of(
                entity.getCounterpartyId(),
                entity.getCounterpartyName(),
                entity.getCounterpartyLei()
            ),
            MonetaryAmount.of(entity.getExposureAmount(), entity.getCurrencyCode()),
            ExposureClassification.of(
                entity.getProductType(),
                InstrumentType.valueOf(entity.getInstrumentType()),
                BalanceSheetType.valueOf(entity.getBalanceSheetType()),
                entity.getCountryCode()
            ),
            entity.getRecordedAt()
        );
    }
}
