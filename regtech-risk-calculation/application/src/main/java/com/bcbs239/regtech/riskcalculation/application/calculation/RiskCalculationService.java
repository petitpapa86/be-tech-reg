package com.bcbs239.regtech.riskcalculation.application.calculation;

import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import com.bcbs239.regtech.riskcalculation.domain.classification.ExposureClassifier;
import com.bcbs239.regtech.riskcalculation.domain.exposure.ExposureRecording;
import com.bcbs239.regtech.riskcalculation.domain.protection.Mitigation;
import com.bcbs239.regtech.riskcalculation.domain.protection.ProtectedExposure;
import com.bcbs239.regtech.riskcalculation.domain.protection.RawMitigationData;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import com.bcbs239.regtech.riskcalculation.domain.valuation.EurAmount;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExchangeRateProvider;
import com.bcbs239.regtech.riskcalculation.domain.valuation.ExposureValuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for orchestrating risk calculation across bounded contexts
 * Coordinates: Valuation → Protection → Classification → Analysis
 * Requirements: 2.1, 3.1, 4.1, 5.1, 6.1
 */
@Service
public class RiskCalculationService {
    
    private static final Logger log = LoggerFactory.getLogger(RiskCalculationService.class);
    
    private final ExchangeRateProvider exchangeRateProvider;
    private final ExposureClassifier classifier;
    
    public RiskCalculationService(
        ExchangeRateProvider exchangeRateProvider,
        ExposureClassifier classifier
    ) {
        this.exchangeRateProvider = exchangeRateProvider;
        this.classifier = classifier;
    }
    
    /**
     * Orchestrates risk calculation across all bounded contexts
     * Flow: Valuation → Protection → Classification → Analysis
     * 
     * @param batchId Unique batch identifier
     * @param exposures List of exposure recordings
     * @param mitigations Map of mitigations grouped by exposure ID
     * @return Portfolio analysis with concentration metrics
     */
    public PortfolioAnalysis calculateRisk(
        String batchId,
        List<ExposureRecording> exposures,
        Map<ExposureId, List<RawMitigationData>> mitigations
    ) {
        log.info("Starting risk calculation for batch: {}, exposures: {}", batchId, exposures.size());
        
        // Step 1: Valuation - Convert to EUR
        log.debug("Step 1: Converting exposures to EUR");
        Map<ExposureId, ExposureValuation> valuations = exposures.stream()
            .collect(Collectors.toMap(
                ExposureRecording::getId,
                exp -> ExposureValuation.convert(
                    exp.getId(),
                    exp.getExposureAmount(),
                    exchangeRateProvider
                )
            ));
        log.debug("Valuation completed: {} exposures converted", valuations.size());
        
        // Step 2: Protection - Apply mitigations
        log.debug("Step 2: Applying credit risk mitigations");
        Map<ExposureId, ProtectedExposure> protectedExposures = exposures.stream()
            .collect(Collectors.toMap(
                ExposureRecording::getId,
                exp -> {
                    EurAmount grossExposure = valuations.get(exp.getId()).getConverted();
                    List<Mitigation> mits = convertMitigations(
                        mitigations.getOrDefault(exp.getId(), List.of())
                    );
                    return ProtectedExposure.calculate(exp.getId(), grossExposure, mits);
                }
            ));
        log.debug("Protection completed: {} exposures with net exposure calculated", protectedExposures.size());
        
        // Step 3: Classification - Classify by region and sector
        log.debug("Step 3: Classifying exposures by region and sector");
        List<ClassifiedExposure> classified = exposures.stream()
            .map(exp -> new ClassifiedExposure(
                exp.getId(),
                protectedExposures.get(exp.getId()).getNetExposure(),
                classifier.classifyRegion(exp.getClassification().countryCode()),
                classifier.classifySector(exp.getClassification().productType())
            ))
            .toList();
        log.debug("Classification completed: {} exposures classified", classified.size());
        
        // Step 4: Analysis - Calculate concentration metrics
        log.debug("Step 4: Calculating portfolio analysis and concentration metrics");
        PortfolioAnalysis analysis = PortfolioAnalysis.analyze(batchId, classified);
        log.info("Risk calculation completed for batch: {}, total portfolio: {} EUR", 
            batchId, analysis.getTotalPortfolio().value());
        
        return analysis;
    }
    
    /**
     * Helper method to convert raw mitigation data to domain mitigations
     * Handles currency conversion for each mitigation
     */
    private List<Mitigation> convertMitigations(List<RawMitigationData> rawMitigations) {
        return rawMitigations.stream()
            .map(raw -> new Mitigation(
                raw.type(),
                raw.value(),
                raw.currency(),
                exchangeRateProvider
            ))
            .toList();
    }
}