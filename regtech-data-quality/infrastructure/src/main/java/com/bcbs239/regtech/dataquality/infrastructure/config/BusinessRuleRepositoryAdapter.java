package com.bcbs239.regtech.dataquality.infrastructure.config;

import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.engine.DefaultRulesEngine;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.BusinessRuleEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.entities.RuleParameterEntity;
import com.bcbs239.regtech.dataquality.infrastructure.rulesengine.repository.BusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.BusinessRuleDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.IBusinessRuleRepository;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleParameterDto;
import com.bcbs239.regtech.dataquality.rulesengine.domain.RuleType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Transactional adapter that maps JPA entities to DTOs while an active session exists.
 *
 * This avoids LazyInitializationException without relying on @EntityGraph and also
 * prevents reloading/rerendering rule DTOs for every exposure by caching them with TTL.
 */
@Transactional(readOnly = true)
public class BusinessRuleRepositoryAdapter implements IBusinessRuleRepository {

    private final BusinessRuleRepository infraRepo;
    private final DefaultRulesEngine rulesEngine;
    private final boolean cacheEnabled;
    private final int cacheTtlSeconds;

    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile Instant lastRefresh = Instant.EPOCH;
    private volatile List<BusinessRuleDto> cachedEnabledRules = List.of();

    public BusinessRuleRepositoryAdapter(
        BusinessRuleRepository infraRepo,
        DefaultRulesEngine rulesEngine,
        boolean cacheEnabled,
        int cacheTtlSeconds
    ) {
        this.infraRepo = infraRepo;
        this.rulesEngine = rulesEngine;
        this.cacheEnabled = cacheEnabled;
        this.cacheTtlSeconds = Math.max(1, cacheTtlSeconds);
    }

    @Override
    public List<BusinessRuleDto> findByEnabledTrue() {
        if (!cacheEnabled) {
            return loadEnabledRules();
        }

        Instant now = Instant.now();
        if (!isExpired(now) && !cachedEnabledRules.isEmpty()) {
            return cachedEnabledRules;
        }

        refreshLock.lock();
        try {
            now = Instant.now();
            if (!isExpired(now) && !cachedEnabledRules.isEmpty()) {
                return cachedEnabledRules;
            }

            List<BusinessRuleDto> loaded = loadEnabledRules();
            cachedEnabledRules = List.copyOf(loaded);
            lastRefresh = now;

            // Keep the engine's internal cache consistent with the DTO cache window.
            // This avoids scenarios where DTOs refresh but the engine still serves stale cached entities.
            rulesEngine.refreshCacheNow();

            return cachedEnabledRules;
        } finally {
            refreshLock.unlock();
        }
    }

    @Override
    public Optional<BusinessRuleDto> findByRuleCode(String ruleCode) {
        if (ruleCode == null) {
            return Optional.empty();
        }

        // Use cached list if available/fresh
        if (cacheEnabled && !isExpired(Instant.now()) && !cachedEnabledRules.isEmpty()) {
            return cachedEnabledRules.stream()
                .filter(r -> ruleCode.equals(r.ruleCode()))
                .findFirst();
        }

        return infraRepo.findByRuleCode(ruleCode).map(this::toDto);
    }

    @Override
    public List<BusinessRuleDto> findByRuleTypeAndEnabledTrueOrderByExecutionOrder(RuleType ruleType) {
        return findByEnabledTrue().stream()
            .filter(r -> r.ruleType() == ruleType)
            .sorted(Comparator.comparing(BusinessRuleDto::executionOrder, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    @Override
    public List<BusinessRuleDto> findByRuleCategoryAndEnabledTrue(String category) {
        return findByEnabledTrue().stream()
            .filter(r -> category != null && category.equals(r.ruleCategory()))
            .toList();
    }

    @Override
    public List<BusinessRuleDto> findActiveRules(LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return findByEnabledTrue().stream()
            .filter(r -> r.isApplicableOn(effectiveDate))
            .sorted(Comparator.comparing(BusinessRuleDto::executionOrder, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    private boolean isExpired(Instant now) {
        long ageSeconds = java.time.Duration.between(lastRefresh, now).getSeconds();
        return ageSeconds >= cacheTtlSeconds;
    }

    private List<BusinessRuleDto> loadEnabledRules() {
        // IMPORTANT: toDto() touches lazy collections, so it must run while the
        // repository call's session is active — which is ensured by @Transactional here.
        return infraRepo.findByEnabledTrue().stream()
            .map(this::toDto)
            .toList();
    }

    private BusinessRuleDto toDto(BusinessRuleEntity entity) {
        // Touch lazy collection(s) while the session is open
        List<RuleParameterDto> parameters = entity.getParameters().stream()
            .map(this::toParamDto)
            .toList();

        return new BusinessRuleDto(
            entity.getRuleId(),
            entity.getRegulationId(),
            entity.getTemplateId(),
            entity.getRuleName(),
            entity.getRuleCode(),
            entity.getDescription(),
            entity.getRuleType(),
            entity.getRuleCategory(),
            entity.getSeverity(),
            entity.getBusinessLogic(),
            entity.getExecutionOrder(),
            entity.getEffectiveDate(),
            entity.getExpirationDate(),
            entity.getEnabled(),
            parameters,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCreatedBy()
        );
    }

    private RuleParameterDto toParamDto(RuleParameterEntity p) {
        return new RuleParameterDto(
            p.getParameterId(),
            p.getParameterName(),
            p.getParameterValue(),
            p.getParameterType(),
            p.getDataType(),
            p.getUnit(),
            p.getMinValue(),
            p.getMaxValue(),
            p.getDescription(),
            p.getIsConfigurable()
        );
    }
}
