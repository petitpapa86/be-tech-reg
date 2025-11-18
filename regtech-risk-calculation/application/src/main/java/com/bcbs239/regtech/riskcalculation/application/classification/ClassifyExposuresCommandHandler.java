package com.bcbs239.regtech.riskcalculation.application.classification;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.calculation.CalculatedExposure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Command handler for classifying exposures by geographic region and sector.
 * Uses DDD approach by delegating to domain classification services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassifyExposuresCommandHandler {
    
    private final GeographicClassificationService geographicClassificationService;
    private final SectorClassificationService sectorClassificationService;
    
    /**
     * Handles the classification of exposures.
     * Applies both geographic and sector classification to each exposure.
     * 
     * @param command The classify exposures command
     * @return Result containing the classified exposures or error details
     */
    public Result<List<CalculatedExposure>> handle(ClassifyExposuresCommand command) {
        log.info("Starting classification for {} exposures in batch: {}", 
            command.getExposureCount(), command.batchId().value());
        
        try {
            // Process each exposure for classification
            List<CalculatedExposure> classifiedExposures = command.exposures().stream()
                .map(exposure -> classifyExposure(exposure, command.bankId()))
                .toList();
            
            log.info("Successfully classified {} exposures for batch: {}", 
                classifiedExposures.size(), command.batchId().value());
            
            return Result.success(classifiedExposures);
            
        } catch (Exception e) {
            log.error("Failed to classify exposures for batch: {}", command.batchId().value(), e);
            
            return Result.failure(ErrorDetail.of(
                "EXPOSURE_CLASSIFICATION_ERROR",
                ErrorType.SYSTEM_ERROR,
                "Failed to classify exposures: " + e.getMessage(),
                "exposure.classification.error"
            ));
        }
    }
    
    /**
     * Classifies a single exposure using domain services.
     * Uses DDD approach: ask the domain services to do the work.
     * 
     * @param exposure The exposure to classify
     * @param bankId The bank ID for context
     * @return The classified exposure
     */
    private CalculatedExposure classifyExposure(CalculatedExposure exposure, BankId bankId) {
        // Ask domain services to perform classification
        geographicClassificationService.classify(exposure, bankId);
        sectorClassificationService.classify(exposure);
        
        return exposure;
    }
}