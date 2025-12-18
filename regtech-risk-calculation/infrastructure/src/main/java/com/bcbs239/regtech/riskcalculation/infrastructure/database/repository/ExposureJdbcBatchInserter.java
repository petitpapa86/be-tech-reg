package com.bcbs239.regtech.riskcalculation.infrastructure.database.repository;

import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExposureJdbcBatchInserter {

    public static final int DEFAULT_BATCH_SIZE = 1_000;

    private final JdbcTemplate jdbcTemplate;

    public ExposureJdbcBatchInserter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void batchInsertExposures(String batchId, List<ExposureRecording> exposures) {
        batchInsertExposures(batchId, exposures, DEFAULT_BATCH_SIZE);
    }

    public void batchInsertExposures(String batchId, List<ExposureRecording> exposures, int batchSize) {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("batchId cannot be null/blank");
        }
        if (exposures == null || exposures.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO riskcalculation.exposures (" +
            "exposure_id, batch_id, instrument_id, " +
            "counterparty_id, counterparty_name, counterparty_lei, " +
            "exposure_amount, currency_code, " +
            "product_type, instrument_type, balance_sheet_type, country_code, " +
            "recorded_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int effectiveBatchSize = (batchSize > 0) ? batchSize : DEFAULT_BATCH_SIZE;
        for (List<ExposureRecording> chunk : chunk(exposures, effectiveBatchSize)) {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ExposureRecording exposure = chunk.get(i);

                    ps.setString(1, exposure.id().value());
                    ps.setString(2, batchId);
                    ps.setString(3, exposure.instrumentId().value());

                    ps.setString(4, exposure.counterparty().counterpartyId());
                    ps.setString(5, exposure.counterparty().name());
                    if (exposure.counterparty().leiCode().isPresent()) {
                        ps.setString(6, exposure.counterparty().leiCode().get());
                    } else {
                        ps.setNull(6, Types.VARCHAR);
                    }

                    BigDecimal amount = exposure.exposureAmount().amount();
                    ps.setBigDecimal(7, amount);
                    ps.setString(8, exposure.exposureAmount().currencyCode());

                    ps.setString(9, exposure.classification().productType());
                    ps.setString(10, exposure.classification().instrumentType().name());
                    ps.setString(11, exposure.classification().balanceSheetType().name());
                    ps.setString(12, exposure.classification().countryCode());

                    ps.setTimestamp(13, ts(exposure.recordedAt()));
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    private static Timestamp ts(Instant instant) {
        return instant != null ? Timestamp.from(instant) : Timestamp.from(Instant.now());
    }

    private static <T> List<List<T>> chunk(List<T> input, int chunkSize) {
        int size = input.size();
        int step = (chunkSize > 0) ? chunkSize : DEFAULT_BATCH_SIZE;

        List<List<T>> result = new ArrayList<>((size / step) + 1);
        for (int i = 0; i < size; i += step) {
            result.add(input.subList(i, Math.min(size, i + step)));
        }
        return result;
    }
}
