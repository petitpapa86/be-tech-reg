package com.bcbs239.regtech.riskcalculation.infrastructure.database.mappers;

import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.calculation.GeographicBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.SectorBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.ConcentrationIndices;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;
import com.bcbs239.regtech.riskcalculation.domain.aggregation.HerfindahlIndex;
import com.bcbs239.regtech.riskcalculation.infrastructure.database.entities.BatchSummaryEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper for converting between BatchSummary domain objects and BatchSummaryEntity JPA entities.
 * Provides centralized mapping logic with proper null handling and validation.
 */
@Component
public class BatchSummaryMapper {

    /**
     * Convert domain aggregate to JPA entity
     */
    public BatchSummaryEntity toEntity(BatchSummary batchSummary) {
        if (batchSummary == null) {
            return null;
        }

        return BatchSummaryEntity.builder()
            .batchSummaryId(batchSummary.getBatchSummaryId().value())
            .batchId(batchSummary.getBatchId().value())
            .bankId(batchSummary.getBankId().value())
            .status(batchSummary.getStatus())
            .createdAt(extractCreatedAt(batchSummary))
            .startedAt(extractStartedAt(batchSummary))
            .completedAt(extractCompletedAt(batchSummary))
            .failedAt(extractFailedAt(batchSummary))
            .totalExposures(extractTotalExposures(batchSummary))
            .totalAmountEur(extractTotalAmountEur(batchSummary))
            .italyAmount(extractItalyAmount(batchSummary))
            .italyPct(extractItalyPct(batchSummary))
            .italyCount(extractItalyCount(batchSummary))
            .euAmount(extractEuAmount(batchSummary))
            .euPct(extractEuPct(batchSummary))
            .euCount(extractEuCount(batchSummary))
            .nonEuAmount(extractNonEuAmount(batchSummary))
            .nonEuPct(extractNonEuPct(batchSummary))
            .nonEuCount(extractNonEuCount(batchSummary))
            .retailAmount(extractRetailAmount(batchSummary))
            .retailPct(extractRetailPct(batchSummary))
            .retailCount(extractRetailCount(batchSummary))
            .sovereignAmount(extractSovereignAmount(batchSummary))
            .sovereignPct(extractSovereignPct(batchSummary))
            .sovereignCount(extractSovereignCount(batchSummary))
            .corporateAmount(extractCorporateAmount(batchSummary))
            .corporatePct(extractCorporatePct(batchSummary))
            .corporateCount(extractCorporateCount(batchSummary))
            .bankingAmount(extractBankingAmount(batchSummary))
            .bankingPct(extractBankingPct(batchSummary))
            .bankingCount(extractBankingCount(batchSummary))
            .otherAmount(extractOtherAmount(batchSummary))
            .otherPct(extractOtherPct(batchSummary))
            .otherCount(extractOtherCount(batchSummary))
            .herfindahlGeographic(extractGeographicHerfindahl(batchSummary))
            .herfindahlSector(extractSectorHerfindahl(batchSummary))
            .resultFileUri(extractResultFileUri(batchSummary))
            .errorMessage(batchSummary.getErrorMessage())
            .build();
    }

    /**
     * Convert JPA entity to domain aggregate
     */
    public BatchSummary toDomain(BatchSummaryEntity entity) {
        if (entity == null) {
            return null;
        }

        ProcessingTimestamps timestamps = new ProcessingTimestamps(
            entity.getStartedAt() != null ? entity.getStartedAt() : entity.getCreatedAt(),
            entity.getCompletedAt(),
            entity.getFailedAt()
        );

        return new BatchSummary(
            BatchSummaryId.of(entity.getBatchSummaryId()),
            BatchId.of(entity.getBatchId()),
            BankId.of(entity.getBankId()),
            entity.getStatus(),
            entity.getTotalExposures() != null ? TotalExposures.of(entity.getTotalExposures()) : null,
            entity.getTotalAmountEur() != null ? TotalAmountEur.of(entity.getTotalAmountEur()) : null,
            buildGeographicBreakdown(entity),
            buildSectorBreakdown(entity),
            buildConcentrationIndices(entity),
            entity.getResultFileUri() != null ? FileStorageUri.of(entity.getResultFileUri()) : null,
            timestamps,
            entity.getErrorMessage()
        );
    }

    // Private extraction methods for entity building
    private java.time.Instant extractCreatedAt(BatchSummary batchSummary) {
        return batchSummary.getTimestamps() != null ? batchSummary.getTimestamps().startedAt() : null;
    }

    private java.time.Instant extractStartedAt(BatchSummary batchSummary) {
        return batchSummary.getTimestamps() != null ? batchSummary.getTimestamps().startedAt() : null;
    }

    private java.time.Instant extractCompletedAt(BatchSummary batchSummary) {
        return batchSummary.getTimestamps() != null ? batchSummary.getTimestamps().completedAt() : null;
    }

    private java.time.Instant extractFailedAt(BatchSummary batchSummary) {
        return batchSummary.getTimestamps() != null ? batchSummary.getTimestamps().failedAt() : null;
    }

    private Integer extractTotalExposures(BatchSummary batchSummary) {
        return batchSummary.getTotalExposures() != null ? batchSummary.getTotalExposures().count() : null;
    }

    private BigDecimal extractTotalAmountEur(BatchSummary batchSummary) {
        return batchSummary.getTotalAmountEur() != null ? batchSummary.getTotalAmountEur().value() : null;
    }

    // Geographic breakdown extraction methods
    private BigDecimal extractItalyAmount(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().italyAmount().value() : null;
    }

    private BigDecimal extractItalyPct(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().italyPercentage().value() : null;
    }

    private Integer extractItalyCount(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().italyCount().count() : null;
    }

    private BigDecimal extractEuAmount(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().euOtherAmount().value() : null;
    }

    private BigDecimal extractEuPct(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().euOtherPercentage().value() : null;
    }

    private Integer extractEuCount(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().euOtherCount().count() : null;
    }

    private BigDecimal extractNonEuAmount(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().nonEuropeanAmount().value() : null;
    }

    private BigDecimal extractNonEuPct(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().nonEuropeanPercentage().value() : null;
    }

    private Integer extractNonEuCount(BatchSummary batchSummary) {
        return batchSummary.getGeographicBreakdown() != null ? 
            batchSummary.getGeographicBreakdown().nonEuropeanCount().count() : null;
    }

    // Sector breakdown extraction methods
    private BigDecimal extractRetailAmount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().retailMortgageAmount().value() : null;
    }

    private BigDecimal extractRetailPct(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().retailMortgagePercentage().value() : null;
    }

    private Integer extractRetailCount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().retailMortgageCount().count() : null;
    }

    private BigDecimal extractSovereignAmount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().sovereignAmount().value() : null;
    }

    private BigDecimal extractSovereignPct(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().sovereignPercentage().value() : null;
    }

    private Integer extractSovereignCount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().sovereignCount().count() : null;
    }

    private BigDecimal extractCorporateAmount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().corporateAmount().value() : null;
    }

    private BigDecimal extractCorporatePct(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().corporatePercentage().value() : null;
    }

    private Integer extractCorporateCount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().corporateCount().count() : null;
    }

    private BigDecimal extractBankingAmount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().bankingAmount().value() : null;
    }

    private BigDecimal extractBankingPct(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().bankingPercentage().value() : null;
    }

    private Integer extractBankingCount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().bankingCount().count() : null;
    }

    private BigDecimal extractOtherAmount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().otherAmount().value() : null;
    }

    private BigDecimal extractOtherPct(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().otherPercentage().value() : null;
    }

    private Integer extractOtherCount(BatchSummary batchSummary) {
        return batchSummary.getSectorBreakdown() != null ? 
            batchSummary.getSectorBreakdown().otherCount().count() : null;
    }

    // Concentration indices extraction methods
    private BigDecimal extractGeographicHerfindahl(BatchSummary batchSummary) {
        return batchSummary.getConcentrationIndices() != null ? 
            batchSummary.getConcentrationIndices().geographicHerfindahl().value() : null;
    }

    private BigDecimal extractSectorHerfindahl(BatchSummary batchSummary) {
        return batchSummary.getConcentrationIndices() != null ? 
            batchSummary.getConcentrationIndices().sectorHerfindahl().value() : null;
    }

    private String extractResultFileUri(BatchSummary batchSummary) {
        return batchSummary.getResultFileUri() != null ? batchSummary.getResultFileUri().uri() : null;
    }

    // Private domain object building methods
    private GeographicBreakdown buildGeographicBreakdown(BatchSummaryEntity entity) {
        if (entity.getItalyAmount() == null || entity.getEuAmount() == null || entity.getNonEuAmount() == null) {
            return null;
        }

        return new GeographicBreakdown(
            TotalAmountEur.of(entity.getItalyAmount()),
            PercentageOfTotal.of(entity.getItalyPct()),
            TotalExposures.of(entity.getItalyCount()),
            TotalAmountEur.of(entity.getEuAmount()),
            PercentageOfTotal.of(entity.getEuPct()),
            TotalExposures.of(entity.getEuCount()),
            TotalAmountEur.of(entity.getNonEuAmount()),
            PercentageOfTotal.of(entity.getNonEuPct()),
            TotalExposures.of(entity.getNonEuCount())
        );
    }

    private SectorBreakdown buildSectorBreakdown(BatchSummaryEntity entity) {
        if (entity.getRetailAmount() == null || entity.getSovereignAmount() == null || 
            entity.getCorporateAmount() == null || entity.getBankingAmount() == null || 
            entity.getOtherAmount() == null) {
            return null;
        }

        return new SectorBreakdown(
            TotalAmountEur.of(entity.getRetailAmount()),
            PercentageOfTotal.of(entity.getRetailPct()),
            TotalExposures.of(entity.getRetailCount()),
            TotalAmountEur.of(entity.getSovereignAmount()),
            PercentageOfTotal.of(entity.getSovereignPct()),
            TotalExposures.of(entity.getSovereignCount()),
            TotalAmountEur.of(entity.getCorporateAmount()),
            PercentageOfTotal.of(entity.getCorporatePct()),
            TotalExposures.of(entity.getCorporateCount()),
            TotalAmountEur.of(entity.getBankingAmount()),
            PercentageOfTotal.of(entity.getBankingPct()),
            TotalExposures.of(entity.getBankingCount()),
            TotalAmountEur.of(entity.getOtherAmount()),
            PercentageOfTotal.of(entity.getOtherPct()),
            TotalExposures.of(entity.getOtherCount())
        );
    }

    private ConcentrationIndices buildConcentrationIndices(BatchSummaryEntity entity) {
        if (entity.getHerfindahlGeographic() == null || entity.getHerfindahlSector() == null) {
            return null;
        }

        return new ConcentrationIndices(
            HerfindahlIndex.of(entity.getHerfindahlGeographic()),
            HerfindahlIndex.of(entity.getHerfindahlSector())
        );
    }
}