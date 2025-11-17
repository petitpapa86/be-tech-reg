package com.bcbs239.regtech.dataquality.application.scoring;

import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.dataquality.domain.quality.QualityWeights;
import com.bcbs239.regtech.dataquality.domain.validation.ValidationResult;

/**
 * Service interface for calculating quality scores based on validation results.
 * Implements weighted scoring across all six quality dimensions.
 * 
 * @deprecated This interface violates DDD principles by putting domain logic in application layer.
 * Use {@link QualityScores#calculateFrom(ValidationResult)} instead - the value object knows how to create itself.
 * This interface will be removed in a future version.
 */
@Deprecated(since = "2.0", forRemoval = true)
public interface QualityScoringEngine {
    
    /**
     * Calculates quality scores based on validation results.
     * Uses configurable weights for each dimension to compute overall score.
     * 
     * @param validationResult The validation results to score
     * @return QualityScores containing individual dimension scores and overall score
     */
    Result<QualityScores> calculateScores(ValidationResult validationResult);
    
    /**
     * Calculates quality scores with custom weights.
     * 
     * @param validationResult The validation results to score
     * @param customWeights Custom weights for dimension scoring
     * @return QualityScores with custom weighted calculation
     */
    Result<QualityScores> calculateScoresWithWeights(
        ValidationResult validationResult, 
        QualityWeights customWeights
    );
}

