package com.bcbs239.regtech.riskcalculation.infrastructure.database.entities;

import com.bcbs239.regtech.riskcalculation.domain.calculation.BatchSummary;
import com.bcbs239.regtech.riskcalculation.domain.calculation.GeographicBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.SectorBreakdown;
import com.bcbs239.regtech.riskcalculation.domain.calculation.ConcentrationIndices;
import com.bcbs239.regtech.riskcalculation.domain.shared.enums.CalculationStatus;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.*;
import com.bcbs239.regtech.riskcalculation.domain.aggregation.HerfindahlIndex;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA Entity for BatchSummary aggregate persistence.
 * Maps domain aggregate to database table structure following the schema design.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_summaries", indexes = {
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_bank_id", columnList = "bank_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class BatchSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "batch_summary_id", unique = true, nullable = false, length = 100)
    private String batchSummaryId;

    @Column(name = "batch_id", unique = true, nullable = false, length = 100)
    private String batchId;

    @Column(name = "bank_id", nullable = false, length = 50)
    private String bankId;

    // Status and timestamps
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CalculationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    // Totals
    @Column(name = "total_exposures")
    private Integer totalExposures;

    @Column(name = "total_amount_eur", precision = 20, scale = 2)
    private BigDecimal totalAmountEur;

    // Geographic breakdown
    @Column(name = "italy_amount", precision = 20, scale = 2)
    private BigDecimal italyAmount;

    @Column(name = "italy_pct", precision = 5, scale = 2)
    private BigDecimal italyPct;

    @Column(name = "italy_count")
    private Integer italyCount;

    @Column(name = "eu_amount", precision = 20, scale = 2)
    private BigDecimal euAmount;

    @Column(name = "eu_pct", precision = 5, scale = 2)
    private BigDecimal euPct;

    @Column(name = "eu_count")
    private Integer euCount;

    @Column(name = "non_eu_amount", precision = 20, scale = 2)
    private BigDecimal nonEuAmount;

    @Column(name = "non_eu_pct", precision = 5, scale = 2)
    private BigDecimal nonEuPct;

    @Column(name = "non_eu_count")
    private Integer nonEuCount;

    // Sector breakdown
    @Column(name = "retail_amount", precision = 20, scale = 2)
    private BigDecimal retailAmount;

    @Column(name = "retail_pct", precision = 5, scale = 2)
    private BigDecimal retailPct;

    @Column(name = "retail_count")
    private Integer retailCount;

    @Column(name = "sovereign_amount", precision = 20, scale = 2)
    private BigDecimal sovereignAmount;

    @Column(name = "sovereign_pct", precision = 5, scale = 2)
    private BigDecimal sovereignPct;

    @Column(name = "sovereign_count")
    private Integer sovereignCount;

    @Column(name = "corporate_amount", precision = 20, scale = 2)
    private BigDecimal corporateAmount;

    @Column(name = "corporate_pct", precision = 5, scale = 2)
    private BigDecimal corporatePct;

    @Column(name = "corporate_count")
    private Integer corporateCount;

    @Column(name = "banking_amount", precision = 20, scale = 2)
    private BigDecimal bankingAmount;

    @Column(name = "banking_pct", precision = 5, scale = 2)
    private BigDecimal bankingPct;

    @Column(name = "banking_count")
    private Integer bankingCount;

    @Column(name = "other_amount", precision = 20, scale = 2)
    private BigDecimal otherAmount;

    @Column(name = "other_pct", precision = 5, scale = 2)
    private BigDecimal otherPct;

    @Column(name = "other_count")
    private Integer otherCount;

    // Concentration metrics
    @Column(name = "herfindahl_geographic", precision = 6, scale = 4)
    private BigDecimal herfindahlGeographic;

    @Column(name = "herfindahl_sector", precision = 6, scale = 4)
    private BigDecimal herfindahlSector;

    // File reference
    @Column(name = "result_file_uri", columnDefinition = "TEXT")
    private String resultFileUri;

    // Error handling
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Convert domain aggregate to JPA entity
     */
    public static BatchSummaryEntity fromDomain(BatchSummary batchSummary) {
        BatchSummaryEntityBuilder builder = BatchSummaryEntity.builder()
            .batchSummaryId(batchSummary.getBatchSummaryId().value())
            .batchId(batchSummary.getBatchId().value())
            .bankId(batchSummary.getBankId().value())
            .status(batchSummary.getStatus())
            .errorMessage(batchSummary.getErrorMessage());

        // Handle timestamps
        if (batchSummary.getTimestamps() != null) {
            ProcessingTimestamps timestamps = batchSummary.getTimestamps();
            builder.createdAt(timestamps.startedAt())
                   .startedAt(timestamps.startedAt())
                   .completedAt(timestamps.completedAt())
                   .failedAt(timestamps.failedAt());
        }

        // Handle totals (only if calculation is complete)
        if (batchSummary.getTotalExposures() != null) {
            builder.totalExposures(batchSummary.getTotalExposures().count());
        }
        if (batchSummary.getTotalAmountEur() != null) {
            builder.totalAmountEur(batchSummary.getTotalAmountEur().value());
        }

        // Handle geographic breakdown
        if (batchSummary.getGeographicBreakdown() != null) {
            GeographicBreakdown geo = batchSummary.getGeographicBreakdown();
            builder.italyAmount(geo.italyAmount().value())
                   .italyPct(geo.italyPercentage().value())
                   .italyCount(geo.italyCount().count())
                   .euAmount(geo.euOtherAmount().value())
                   .euPct(geo.euOtherPercentage().value())
                   .euCount(geo.euOtherCount().count())
                   .nonEuAmount(geo.nonEuropeanAmount().value())
                   .nonEuPct(geo.nonEuropeanPercentage().value())
                   .nonEuCount(geo.nonEuropeanCount().count());
        }

        // Handle sector breakdown
        if (batchSummary.getSectorBreakdown() != null) {
            SectorBreakdown sector = batchSummary.getSectorBreakdown();
            builder.retailAmount(sector.retailMortgageAmount().value())
                   .retailPct(sector.retailMortgagePercentage().value())
                   .retailCount(sector.retailMortgageCount().count())
                   .sovereignAmount(sector.sovereignAmount().value())
                   .sovereignPct(sector.sovereignPercentage().value())
                   .sovereignCount(sector.sovereignCount().count())
                   .corporateAmount(sector.corporateAmount().value())
                   .corporatePct(sector.corporatePercentage().value())
                   .corporateCount(sector.corporateCount().count())
                   .bankingAmount(sector.bankingAmount().value())
                   .bankingPct(sector.bankingPercentage().value())
                   .bankingCount(sector.bankingCount().count())
                   .otherAmount(sector.otherAmount().value())
                   .otherPct(sector.otherPercentage().value())
                   .otherCount(sector.otherCount().count());
        }

        // Handle concentration indices
        if (batchSummary.getConcentrationIndices() != null) {
            ConcentrationIndices indices = batchSummary.getConcentrationIndices();
            builder.herfindahlGeographic(indices.geographicHerfindahl().value())
                   .herfindahlSector(indices.sectorHerfindahl().value());
        }

        // Handle result file URI
        if (batchSummary.getResultFileUri() != null) {
            builder.resultFileUri(batchSummary.getResultFileUri().uri());
        }

        return builder.build();
    }

    /**
     * Convert JPA entity to domain aggregate
     */
    public BatchSummary toDomain() {
        // Create processing timestamps
        ProcessingTimestamps timestamps = new ProcessingTimestamps(
            startedAt != null ? startedAt : createdAt,
            completedAt,
            failedAt
        );

        // Create basic batch summary
        BatchSummary batchSummary = new BatchSummary(
            BatchSummaryId.of(batchSummaryId),
            BatchId.of(batchId),
            BankId.of(bankId),
            status,
            totalExposures != null ? TotalExposures.of(totalExposures) : null,
            totalAmountEur != null ? TotalAmountEur.of(totalAmountEur) : null,
            createGeographicBreakdown(),
            createSectorBreakdown(),
            createConcentrationIndices(),
            resultFileUri != null ? FileStorageUri.of(resultFileUri) : null,
            timestamps,
            errorMessage
        );

        return batchSummary;
    }

    private GeographicBreakdown createGeographicBreakdown() {
        if (italyAmount == null || euAmount == null || nonEuAmount == null) {
            return null;
        }

        return new GeographicBreakdown(
            TotalAmountEur.of(italyAmount),
            PercentageOfTotal.of(italyPct),
            TotalExposures.of(italyCount),
            TotalAmountEur.of(euAmount),
            PercentageOfTotal.of(euPct),
            TotalExposures.of(euCount),
            TotalAmountEur.of(nonEuAmount),
            PercentageOfTotal.of(nonEuPct),
            TotalExposures.of(nonEuCount)
        );
    }

    private SectorBreakdown createSectorBreakdown() {
        if (retailAmount == null || sovereignAmount == null || corporateAmount == null ||
            bankingAmount == null || otherAmount == null) {
            return null;
        }

        return new SectorBreakdown(
            TotalAmountEur.of(retailAmount),
            PercentageOfTotal.of(retailPct),
            TotalExposures.of(retailCount),
            TotalAmountEur.of(sovereignAmount),
            PercentageOfTotal.of(sovereignPct),
            TotalExposures.of(sovereignCount),
            TotalAmountEur.of(corporateAmount),
            PercentageOfTotal.of(corporatePct),
            TotalExposures.of(corporateCount),
            TotalAmountEur.of(bankingAmount),
            PercentageOfTotal.of(bankingPct),
            TotalExposures.of(bankingCount),
            TotalAmountEur.of(otherAmount),
            PercentageOfTotal.of(otherPct),
            TotalExposures.of(otherCount)
        );
    }

    private ConcentrationIndices createConcentrationIndices() {
        if (herfindahlGeographic == null || herfindahlSector == null) {
            return null;
        }

        return new ConcentrationIndices(
            HerfindahlIndex.of(herfindahlGeographic),
            HerfindahlIndex.of(herfindahlSector)
        );
    }
}