package com.bcbs239.regtech.riskcalculation.infrastructure.ingestion;

import com.bcbs239.regtech.core.domain.shared.dto.BatchDataDTO;
import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.core.domain.shared.dto.ExposureDTO;
import com.bcbs239.regtech.riskcalculation.domain.exposure.*;
import com.bcbs239.regtech.riskcalculation.domain.protection.MitigationType;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps DTOs to domain objects
 * Handles conversion from presentation layer to domain layer
 */
@Component
public class RiskReportMapper {
    
    public IngestedRiskReport toDomain(BatchDataDTO dto) {
        // Generate batch ID
        String batchId = generateBatchId();
        
        // Map bank info
        BankInfo bankInfo = mapBankInfo(dto.bankInfo());
        
        // Map exposures
        List<ExposureRecording> exposures = dto.exposures().stream()
            .map(this::mapExposure)
            .toList();
        
        // Map mitigations grouped by exposure ID
        Map<ExposureId, List<RawMitigationData>> mitigations = dto.creditRiskMitigation().stream()
            .collect(Collectors.groupingBy(
                m -> ExposureId.of(m.exposureId()),
                Collectors.mapping(
                    this::mapMitigation,
                    Collectors.toList()
                )
            ));
        
        return new IngestedRiskReport(
            batchId,
            bankInfo,
            exposures,
            mitigations,
            Instant.now()
        );
    }
    
    private BankInfo mapBankInfo(com.bcbs239.regtech.core.domain.shared.dto.BankInfoDTO dto) {
        return BankInfo.of(
            dto.bankName(),
            dto.abiCode(),
            dto.leiCode()
        );
    }
    
    private ExposureRecording mapExposure(ExposureDTO dto) {
        ExposureId id = ExposureId.of(dto.exposureId());
        InstrumentId instrumentId = new InstrumentId(dto.instrumentId());
        
        CounterpartyRef counterparty = new CounterpartyRef(
            dto.counterpartyId(),
            dto.counterpartyName(),
            Optional.ofNullable(dto.counterpartyLei())
        );
        
        MonetaryAmount amount = new MonetaryAmount(
            dto.exposureAmount(),
            dto.currency()
        );
        
        ExposureClassification classification = new ExposureClassification(
            dto.productType(),
            InstrumentType.valueOf(dto.instrumentType()),
            BalanceSheetType.valueOf(dto.balanceSheetType()),
            dto.countryCode()
        );
        
        return new ExposureRecording(
            id,
            instrumentId,
            counterparty,
            amount,
            classification,
            Instant.now()
        );
    }
    
    private RawMitigationData mapMitigation(CreditRiskMitigationDTO dto) {
        return new RawMitigationData(
            MitigationType.valueOf(dto.mitigationType()),
            dto.value(),
            dto.currency()
        );
    }
    
    private String generateBatchId() {
        return "batch_" + Instant.now().toString().replace(":", "").replace(".", "_");
    }
}
