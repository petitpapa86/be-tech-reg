package com.bcbs239.regtech.riskcalculation.infrastructure.ingestion;

import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.BankInfo;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing an ingested and validated risk report
 * Contains domain objects ready for risk calculation
 */
public record IngestedRiskReport(String batchId, BankInfo bankInfo, List<ExposureRecording> exposures,
                                 Map<ExposureId, List<RawMitigationData>> mitigations, Instant ingestedAt) {

    public IngestedRiskReport(
            String batchId,
            BankInfo bankInfo,
            List<ExposureRecording> exposures,
            Map<ExposureId, List<RawMitigationData>> mitigations,
            Instant ingestedAt
    ) {
        this.batchId = Objects.requireNonNull(batchId, "Batch ID cannot be null");
        this.bankInfo = Objects.requireNonNull(bankInfo, "Bank info cannot be null");
        this.exposures = Objects.requireNonNull(exposures, "Exposures cannot be null");
        this.mitigations = Objects.requireNonNull(mitigations, "Mitigations cannot be null");
        this.ingestedAt = Objects.requireNonNull(ingestedAt, "Ingested timestamp cannot be null");
    }

    public int getTotalExposures() {
        return exposures.size();
    }
}
