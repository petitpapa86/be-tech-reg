package com.bcbs239.regtech.modules.dataquality.application.services;

import com.bcbs239.regtech.core.shared.Result;
import com.bcbs239.regtech.modules.dataquality.domain.quality.QualityScores;
import com.bcbs239.regtech.modules.dataquality.domain.validation.ValidationResult;

/**
 * Service interface for calculating quality scores based on validation results.
 * Implements weighted scoring across all six quality dimensions.
 */
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
        com.bcbs239.regtech.modules.dataquality.domain.quality.QualityWeights customWeights
    );
}