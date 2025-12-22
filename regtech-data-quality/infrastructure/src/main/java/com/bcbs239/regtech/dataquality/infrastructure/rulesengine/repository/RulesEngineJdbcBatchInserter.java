package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import com.bcbs239.regtech.dataquality.domain.rules.RuleViolation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RulesEngineJdbcBatchInserter {

    private static final int DEFAULT_CHUNK_SIZE = 1_000;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public RulesEngineJdbcBatchInserter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void insertViolations(String batchId, List<RuleViolation> violations) {
        insertViolations(batchId, violations, DEFAULT_CHUNK_SIZE);
    }

    public void insertViolations(String batchId, List<RuleViolation> violations, int chunkSize) {
        if (violations == null || violations.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO dataquality.rule_violations (" +
            "rule_id, execution_id, batch_id, entity_type, entity_id, " +
            "violation_type, violation_description, severity, detected_at, violation_details, resolution_status" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)";

        for (List<RuleViolation> chunk : chunk(violations, chunkSize)) {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    RuleViolation v = chunk.get(i);

                    ps.setString(1, v.ruleId());

                    Long executionId = normalizeExecutionId(v.executionId());
                    if (executionId != null) {
                        ps.setLong(2, executionId);
                    } else {
                        ps.setNull(2, Types.BIGINT);
                    }

                    if (batchId != null) {
                        ps.setString(3, batchId);
                    } else {
                        ps.setNull(3, Types.VARCHAR);
                    }

                    ps.setString(4, v.entityType());
                    ps.setString(5, v.entityId());
                    ps.setString(6, v.violationType());
                    ps.setString(7, v.violationDescription());
                    ps.setString(8, v.severity() != null ? v.severity().name() : null);
                    ps.setTimestamp(9, ts(v.detectedAt()));

                    setJsonbString(ps, 10, v.violationDetails());

                    ps.setString(11, v.resolutionStatus() != null ? v.resolutionStatus().name() : null);
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }

    private Timestamp ts(Instant instant) {
        return instant != null ? Timestamp.from(instant) : Timestamp.from(Instant.now());
    }

    private void setJsonbString(PreparedStatement ps, int paramIndex, Map<String, Object> value) throws SQLException {
        if (value == null) {
            ps.setNull(paramIndex, Types.VARCHAR);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            ps.setString(paramIndex, json);
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize JSONB parameter", e);
        }
    }

    private static Long normalizeExecutionId(Long executionId) {
        return (executionId != null && executionId > 0) ? executionId : null;
    }

    private static <T> List<List<T>> chunk(List<T> input, int chunkSize) {
        int size = input.size();
        int step = chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;

        List<List<T>> result = new ArrayList<>((size / step) + 1);
        for (int i = 0; i < size; i += step) {
            result.add(input.subList(i, Math.min(size, i + step)));
        }
        return result;
    }
}
