package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.dto.CreditRiskMitigationDTO;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.protection.Mitigation;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExposureValuation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Domain service for processing exposures through the risk calculation pipeline.
 * 
 * This service orchestrates the transformation of raw exposures through:
 * 1. Currency conversion to EUR
 * 2. Application of credit risk mitigations
 * 3. Classification by region and sector
 * 
 * Following DDD principles:
 * - Domain service for coordination across multiple aggregates
 * - Uses "Tell, Don't Ask" - delegates to domain objects
 * - Encapsulates the processing pipeline logic
 * - Stateless - no internal state, only coordinates behavior
 * 
 * This eliminates the procedural for-loop in the application layer,
 * moving the orchestration into the domain where it belongs.
 * 
 * Requirements: 6.1 (Exposure Processing), 7.1 (Risk Calculation)
 */
public class ExposureProcessingService {
    
    private final ExchangeRateProvider exchangeRateProvider;
    private final ExposureClassifier exposureClassifier;
    
    public ExposureProcessingService(
        ExchangeRateProvider exchangeRateProvider,
        ExposureClassifier exposureClassifier
    ) {
        this.exchangeRateProvider = exchangeRateProvider;
        this.exposureClassifier = exposureClassifier;
    }
    
    /**
     * Process a collection of exposures through the complete risk calculation pipeline.
     * 
     * This method coordinates the transformation:
     * ExposureRecording -> EUR conversion -> Mitigation -> ProtectedExposure -> Classification -> ClassifiedExposure
     * 
     * Domain objects tell us what they can do:
     * - ExposureValuation.convert() converts currencies
     * - Mitigation.fromDTO() creates mitigations
     * - ProtectedExposure.calculate() applies mitigations
     * - ExposureClassifier classifies by region/sector
     * - ClassifiedExposure.of() creates classified results
     * 
     * @param exposures The raw exposure recordings to process
     * @param mitigationsByExposure Map of mitigations grouped by exposure ID
     * @return Processed results containing both protected and classified exposures
     */
    public ProcessingResult processExposures(
        List<ExposureRecording> exposures,
        Map<String, List<CreditRiskMitigationDTO>> mitigationsByExposure
    ) {
        // Use stream to declaratively process each exposure through the pipeline
        List<ProcessedExposure> processed = exposures.stream()
            .map(exposure -> processExposure(exposure, mitigationsByExposure))
            .collect(Collectors.toList());
        
        // Extract the results
        List<ProtectedExposure> protectedExposures = processed.stream()
            .map(ProcessedExposure::protectedExposure)
            .collect(Collectors.toList());
        
        List<ClassifiedExposure> classifiedExposures = processed.stream()
            .map(ProcessedExposure::classifiedExposure)
            .collect(Collectors.toList());
        
        return new ProcessingResult(protectedExposures, classifiedExposures);
    }
    
    /**
     * Process a single exposure through the complete pipeline.
     * 
     * Domain objects do the work (Tell, Don't Ask):
     * - ExposureValuation.convert() handles currency conversion
     * - Mitigation.fromDTO() creates domain mitigations
     * - ProtectedExposure.calculate() applies mitigations
     * - ExposureClassifier classifies region and sector
     * - ClassifiedExposure.of() creates the final result
     * 
     * @param exposure The exposure to process
     * @param mitigationsByExposure Map of available mitigations
     * @return Processed exposure with both protection and classification applied
     */
    private ProcessedExposure processExposure(
        ExposureRecording exposure,
        Map<String, List<CreditRiskMitigationDTO>> mitigationsByExposure
    ) {
        // Step 1: Convert to EUR - domain object knows how
        ExposureValuation eurValuation = ExposureValuation.convert(
            exposure.id(), 
            exposure.exposureAmount(), 
            exchangeRateProvider
        );
        
        // Step 2: Get and convert mitigations - domain objects know how
        List<Mitigation> mitigations = mitigationsByExposure
            .getOrDefault(exposure.id().value(), List.of())
            .stream()
            .map(dto -> Mitigation.fromDTO(dto, exchangeRateProvider))
            .collect(Collectors.toList());
        
        // Step 3: Calculate protected exposure - domain object knows how
        ProtectedExposure protectedExposure = ProtectedExposure.calculate(
            exposure.id(), 
            eurValuation.getConverted(), 
            mitigations
        );
        
        // Step 4: Classify exposure - domain service knows how
        var region = exposureClassifier.classifyRegion(exposure.classification().countryCode());
        var sector = exposureClassifier.classifySector(exposure.classification().productType());
        
        // Step 5: Create classified exposure - domain object knows how
        ClassifiedExposure classifiedExposure = ClassifiedExposure.of(
            exposure.id(), 
            protectedExposure.getNetExposure(), 
            region, 
            sector
        );
        
        return new ProcessedExposure(protectedExposure, classifiedExposure);
    }
    
    /**
     * Internal record to hold both protected and classified results for a single exposure.
     */
    private record ProcessedExposure(
        ProtectedExposure protectedExposure,
        ClassifiedExposure classifiedExposure
    ) {}
    
    /**
     * Result of exposure processing containing both protected and classified exposures.
     * 
     * @param protectedExposures Exposures with mitigations applied
     * @param classifiedExposures Exposures classified by region and sector
     */
    public record ProcessingResult(
        List<ProtectedExposure> protectedExposures,
        List<ClassifiedExposure> classifiedExposures
    ) {}
}
