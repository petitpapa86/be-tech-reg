package com.bcbs239.regtech.dataquality.infrastructure.rulesengine;

import com.bcbs239.regtech.dataquality.application.rulesengine.RuleViolationRepository;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleViolation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * JPA adapter for RuleViolationRepository port.
 */
@Component
public class JpaRuleViolationRepositoryAdapter implements RuleViolationRepository {

    private final com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository jpaRepository;

    public JpaRuleViolationRepositoryAdapter(
            com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleViolationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(RuleViolation violation) {
        RuleViolationEntity entity = mapToEntity(violation);
        jpaRepository.save(entity);
    }

    @Override
    public void saveAllForBatch(String batchId, List<RuleViolation> violations) {
        List<RuleViolationEntity> entities = violations.stream()
            .map(this::mapToEntity)
            .peek(entity -> entity.setBatchId(batchId))
            .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    private RuleViolationEntity mapToEntity(RuleViolation violation) {
        RuleViolationEntity entity = new RuleViolationEntity();
        entity.setRuleId(violation.ruleId());
        entity.setExecutionId(violation.executionId());
        entity.setEntityType(violation.entityType());
        entity.setEntityId(violation.entityId());
        entity.setViolationType(violation.violationType());
        entity.setViolationDescription(violation.violationDescription());
        entity.setSeverity(violation.severity());
        entity.setDetectedAt(violation.detectedAt());
        entity.setViolationDetails(violation.violationDetails());
        entity.setResolutionStatus(violation.resolutionStatus());
        return entity;
    }
}