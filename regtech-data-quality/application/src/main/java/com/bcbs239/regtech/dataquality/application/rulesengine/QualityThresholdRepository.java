package com.bcbs239.regtech.dataquality.application.rulesengine;

import com.bcbs239.regtech.dataquality.domain.quality.QualityThreshold;
import java.util.List;
import java.util.Optional;

/**
 * Port for quality threshold persistence.
 */
public interface QualityThresholdRepository {

    /**
     * Saves quality thresholds.
     */
    void save(QualityThreshold threshold);

    /**
     * Finds the current quality thresholds for a specific bank.
     */
    Optional<QualityThreshold> findByBankId(String bankId);

    /**
     * Loads all quality thresholds.
     */
    List<QualityThreshold> findAll();
}
