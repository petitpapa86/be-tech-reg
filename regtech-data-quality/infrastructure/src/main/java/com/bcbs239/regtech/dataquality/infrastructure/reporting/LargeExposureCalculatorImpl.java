package com.bcbs239.regtech.dataquality.infrastructure.reporting;

import com.bcbs239.regtech.dataquality.application.reporting.LargeExposureCalculator;
import com.bcbs239.regtech.dataquality.domain.model.reporting.DetailedExposureResult;
import com.bcbs239.regtech.dataquality.domain.model.reporting.StoredValidationResults;
import com.bcbs239.regtech.dataquality.application.reporting.StoredValidationResultsReader;
import com.bcbs239.regtech.dataquality.domain.model.valueobject.LargeExposure;
import com.bcbs239.regtech.dataquality.domain.report.QualityReport;
import com.bcbs239.regtech.core.domain.shared.valueobjects.BatchId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of {@link LargeExposureCalculator}.
 *
 * <p>Uses the risk calculation exposure table (schema: riskcalculation.exposures)
 * as the data source for counterparty names and exposure amounts.</p>
 */
@Component
public class LargeExposureCalculatorImpl implements LargeExposureCalculator {

    private final JdbcTemplate jdbcTemplate;
    private final StoredValidationResultsReader detailedResultsReader;
    private final BigDecimal tierOneCapital;

    public LargeExposureCalculatorImpl(
        JdbcTemplate jdbcTemplate,
        StoredValidationResultsReader detailedResultsReader,
        @Value("${data-quality.reporting.tier-one-capital:2500000000}") BigDecimal tierOneCapital
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.detailedResultsReader = Objects.requireNonNull(detailedResultsReader, "detailedResultsReader cannot be null");
        this.tierOneCapital = Objects.requireNonNull(tierOneCapital, "tierOneCapital cannot be null");
        if (this.tierOneCapital.signum() <= 0) {
            throw new IllegalArgumentException("tierOneCapital must be positive");
        }
    }

    @Override
    public List<LargeExposure> calculate(QualityReport report) {
        Objects.requireNonNull(report, "report cannot be null");

        BatchId batchId = report.getBatchId();
        if (batchId == null || batchId.value() == null || batchId.value().isBlank()) {
            return List.of();
        }

        // If we have a local details URI, use it to get exposureIds that were actually validated.
        // This keeps the calculation aligned with the same "universe" as the validation output.
        // If the list is large, fall back to batch aggregation (more efficient).
        List<String> exposureIdsFromDetails = loadExposureIdsFromDetailsIfAvailable(report);
        if (!exposureIdsFromDetails.isEmpty() && exposureIdsFromDetails.size() <= 2000) {
            return calculateFromExposureIds(batchId, exposureIdsFromDetails);
        }

        // Group exposures by counterparty for this batch.
        // We use MIN(exposure_id) as a stable identifier for the aggregated row.
        String sql = """
            SELECT
              MIN(exposure_id) AS exposure_id,
              counterparty_name,
              SUM(exposure_amount) AS total_amount
            FROM riskcalculation.exposures
            WHERE batch_id = ?
            GROUP BY counterparty_name
            ORDER BY total_amount DESC
            """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                String exposureId = rs.getString("exposure_id");
                String counterparty = rs.getString("counterparty_name");
                BigDecimal amount = rs.getBigDecimal("total_amount");

                BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
                double percentOfCapital = safeAmount.signum() <= 0
                    ? 0.0
                    : safeAmount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(tierOneCapital, 4, java.math.RoundingMode.HALF_UP)
                        .doubleValue();

                return new LargeExposure(
                    exposureId != null ? exposureId : ("BATCH-" + batchId.value() + "-" + rowNum),
                    counterparty != null ? counterparty : "Controparte",
                    safeAmount,
                    percentOfCapital,
                    tierOneCapital
                );
            },
            batchId.value()
        );
    }

    private List<String> loadExposureIdsFromDetailsIfAvailable(QualityReport report) {
        if (report.getDetailsReference() == null || report.getDetailsReference().uri() == null) {
            return List.of();
        }

        String detailsUri = report.getDetailsReference().uri();
        StoredValidationResults stored = detailedResultsReader.load(detailsUri).orElse(null);
        List<DetailedExposureResult> detailed = stored != null ? stored.exposureResults() : List.of();
        if (detailed == null || detailed.isEmpty()) {
            return List.of();
        }

        return detailed.stream()
            .map(DetailedExposureResult::exposureId)
            .filter(Objects::nonNull)
            .filter(s -> !s.isBlank())
            .distinct()
            .collect(Collectors.toList());
    }

    private List<LargeExposure> calculateFromExposureIds(BatchId batchId, List<String> exposureIds) {
        // Chunk to avoid overly long SQL statements.
        final int chunkSize = 500;

        Map<String, CounterpartyAggregation> byCounterparty = new HashMap<>();

        for (int i = 0; i < exposureIds.size(); i += chunkSize) {
            List<String> chunk = exposureIds.subList(i, Math.min(i + chunkSize, exposureIds.size()));

            String placeholders = chunk.stream().map(x -> "?").collect(Collectors.joining(","));
            String sql = "SELECT exposure_id, counterparty_name, exposure_amount " +
                "FROM riskcalculation.exposures WHERE batch_id = ? AND exposure_id IN (" + placeholders + ")";

            List<Object> args = new ArrayList<>(1 + chunk.size());
            args.add(batchId.value());
            args.addAll(chunk);

            jdbcTemplate.query(sql, rs -> {
                String exposureId = rs.getString("exposure_id");
                String counterparty = rs.getString("counterparty_name");
                BigDecimal amount = rs.getBigDecimal("exposure_amount");

                String safeCounterparty = (counterparty == null || counterparty.isBlank()) ? "Controparte" : counterparty;
                BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;

                byCounterparty.compute(safeCounterparty, (k, existing) -> {
                    if (existing == null) {
                        return new CounterpartyAggregation(exposureId, safeAmount);
                    }
                    BigDecimal newTotal = existing.totalAmount.add(safeAmount);
                    String stableId = existing.stableExposureId != null ? existing.stableExposureId : exposureId;
                    return new CounterpartyAggregation(stableId, newTotal);
                });
            }, args.toArray());
        }

        return byCounterparty.entrySet().stream()
            .sorted((a, b) -> b.getValue().totalAmount.compareTo(a.getValue().totalAmount))
            .map(entry -> {
                String counterparty = entry.getKey();
                CounterpartyAggregation agg = entry.getValue();
                BigDecimal amount = agg.totalAmount;
                double percentOfCapital = amount.signum() <= 0
                    ? 0.0
                    : amount
                        .multiply(BigDecimal.valueOf(100))
                        .divide(tierOneCapital, 4, java.math.RoundingMode.HALF_UP)
                        .doubleValue();

                return new LargeExposure(
                    agg.stableExposureId != null ? agg.stableExposureId : ("BATCH-" + batchId.value() + "-" + counterparty.hashCode()),
                    counterparty,
                    amount,
                    percentOfCapital,
                    tierOneCapital
                );
            })
            .collect(Collectors.toList());
    }

    private record CounterpartyAggregation(String stableExposureId, BigDecimal totalAmount) {
        private CounterpartyAggregation {
            if (totalAmount == null) {
                totalAmount = BigDecimal.ZERO;
            }
        }
    }
}
