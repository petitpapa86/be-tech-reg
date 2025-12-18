package com.bcbs239.regtech.dataquality.infrastructure.rulesengine;

import com.bcbs239.regtech.dataquality.application.rulesengine.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleExecutionLogEntity;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleExecutionLogDto;

import java.util.List;

/**
 * JPA adapter for RuleExecutionLogRepository port.
 */
public class JpaRuleExecutionLogRepositoryAdapter implements RuleExecutionLogRepository {

    private final com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository jpaRepository;

    public JpaRuleExecutionLogRepositoryAdapter(
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(RuleExecutionLogDto executionLog) {
        RuleExecutionLogEntity entity = mapToEntity(executionLog);
        jpaRepository.save(entity);
    }

    @Override
    public void saveAllForBatch(String batchId, List<RuleExecutionLogDto> executionLogs) {
        List<RuleExecutionLogEntity> entities = executionLogs.stream()
            .map(this::mapToEntity)
            .peek(entity -> entity.setBatchId(batchId))
            .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    private RuleExecutionLogEntity mapToEntity(RuleExecutionLogDto dto) {
        RuleExecutionLogEntity entity = new RuleExecutionLogEntity();
        entity.setRuleId(dto.ruleId());
        entity.setExecutionTimestamp(dto.executionTimestamp());
        entity.setEntityType(dto.entityType());
        entity.setEntityId(dto.entityId());
        entity.setExecutionResult(dto.executionResult());
        entity.setViolationCount(dto.violationCount());
        entity.setExecutionTimeMs(dto.executionTimeMs());
        entity.setErrorMessage(dto.errorMessage());
        entity.setExecutedBy(dto.executedBy());
        return entity;
    }
}