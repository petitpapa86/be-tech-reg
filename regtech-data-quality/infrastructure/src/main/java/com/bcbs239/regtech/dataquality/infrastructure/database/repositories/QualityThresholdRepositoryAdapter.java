package com.bcbs239.regtech.dataquality.infrastructure.database.repositories;

import com.bcbs239.regtech.dataquality.application.rulesengine.QualityThresholdRepository;
import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import com.bcbs239.regtech.dataquality.infrastructure.database.entities.QualityThresholdEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class QualityThresholdRepositoryAdapter implements QualityThresholdRepository {
    private final QualityThresholdJpaRepository jpaRepository;

    @Override
    public void save(QualityThreshold threshold) {
        // Deactivate old thresholds if we only want one active at a time for this bank
        // Or just save as a new version
        QualityThresholdEntity entity = QualityThresholdEntity.builder()
                .bankId(threshold.bankId())
                .completenessMinPercent(threshold.completenessMinPercent())
                .accuracyMaxErrorPercent(threshold.accuracyMaxErrorPercent())
                .timelinessDays(threshold.timelinessDays())
                .consistencyPercent(threshold.consistencyPercent())
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public Optional<QualityThreshold> findByBankId(String bankId) {
        return jpaRepository.findFirstByBankIdAndIsActiveTrueOrderByCreatedAtDesc(bankId)
                .map(this::toDomain);
    }

    @Override
    public List<QualityThreshold> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private QualityThreshold toDomain(QualityThresholdEntity entity) {
        return new QualityThreshold(
                entity.getBankId(),
                entity.getCompletenessMinPercent(),
                entity.getAccuracyMaxErrorPercent(),
                entity.getTimelinessDays(),
                entity.getConsistencyPercent()
        );
    }
}
