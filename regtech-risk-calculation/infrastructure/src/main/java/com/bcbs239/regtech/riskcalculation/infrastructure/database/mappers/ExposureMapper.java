package com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers;

import com.bcbs239.regtech.riskcalculation.domain.exposure.*;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.ExposureEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ExposureRecording domain model and ExposureEntity
 */
@Component("exposureEntityMapper")
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
            .exposureId(exposure.id().value())
            .batchId(batchId)
            .instrumentId(exposure.instrumentId().value())
            .counterpartyId(exposure.counterparty().counterpartyId())
            .counterpartyName(exposure.counterparty().name())
            .counterpartyLei(exposure.counterparty().leiCode().orElse(null))
            .exposureAmount(exposure.exposureAmount().amount())
            .currencyCode(exposure.exposureAmount().currencyCode())
            .productType(exposure.classification().productType())
            .instrumentType(exposure.classification().instrumentType().name())
            .balanceSheetType(exposure.classification().balanceSheetType().name())
            .countryCode(exposure.classification().countryCode())
            .recordedAt(exposure.recordedAt())
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
