package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine;

import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleExecutionLogEntity;
import com.bcbs239.regtech.dataquality.infrastructure.database.entities.RuleViolationEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.RuleExecutionLogRepository;
import com.bcbs239.regtech.dataquality.infrastructure.database.repositories.RuleViolationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists rules engine audit data (execution logs / violations) in an isolated transaction.
 *
 * This prevents best-effort logging from marking the caller transaction rollback-only when
 * a JPA exception occurs.
 */
@Service
public class RulesEngineAuditPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(RulesEngineAuditPersistenceService.class);

    private final RuleExecutionLogRepository executionLogRepository;
    private final RuleViolationRepository violationRepository;

    public RulesEngineAuditPersistenceService(
        RuleExecutionLogRepository executionLogRepository,
        RuleViolationRepository violationRepository
    ) {
        this.executionLogRepository = executionLogRepository;
        this.violationRepository = violationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RuleExecutionLogEntity saveExecutionLogBestEffort(RuleExecutionLogEntity executionLog) {
        try {
            return executionLogRepository.saveAndFlush(executionLog);
        } catch (Exception e) {
            log.warn(
                "Failed to save rule execution log for rule {}: {}. Logging is best-effort, continuing execution.",
                executionLog != null ? executionLog.getRuleId() : "<null>",
                e.getMessage()
            );
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveViolationBestEffort(RuleViolationEntity violation) {
        try {
            violationRepository.saveAndFlush(violation);
        } catch (Exception e) {
            log.warn(
                "Failed to save rule violation for rule {}: {}. Logging is best-effort, continuing execution.",
                violation != null ? violation.getRuleId() : "<null>",
                e.getMessage()
            );
        }
    }
}
