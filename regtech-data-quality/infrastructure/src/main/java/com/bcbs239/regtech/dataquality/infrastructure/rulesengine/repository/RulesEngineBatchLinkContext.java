package com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Thread-scoped link context used to connect execution logs to violations during batch persistence.
 *
 * <p>We persist execution logs first (generating execution_id), then persist violations that reference
 * the corresponding execution_id via FK. This context stores the mapping per request/thread.</p>
 */
@Component
public class RulesEngineBatchLinkContext {

    private static final ThreadLocal<Map<String, Long>> EXECUTION_ID_BY_KEY = ThreadLocal.withInitial(HashMap::new);

    public void clear() {
        EXECUTION_ID_BY_KEY.get().clear();
    }

    public void putExecutionId(String ruleId, String entityType, String entityId, Long executionId) {
        if (executionId == null) {
            return;
        }
        EXECUTION_ID_BY_KEY.get().put(key(ruleId, entityType, entityId), executionId);
    }

    public Long getExecutionId(String ruleId, String entityType, String entityId) {
        return EXECUTION_ID_BY_KEY.get().get(key(ruleId, entityType, entityId));
    }

    private String key(String ruleId, String entityType, String entityId) {
        return String.join(
            "|",
            Objects.toString(ruleId, ""),
            Objects.toString(entityType, ""),
            Objects.toString(entityId, "")
        );
    }
}
