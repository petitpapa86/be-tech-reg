package com.bcbs239.regtech.riskcalculation.infrastructure.ingestion;

import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationResult;
import com.bcbs239.regtech.riskcalculation.application.calculation.RiskCalculationService;
import com.bcbs239.regtech.riskcalculation.application.shared.InvalidReportException;
import com.bcbs239.regtech.riskcalculation.infrastructure.dto.RiskReportDTO;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.shared.valueobjects.ExposureId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for ingesting and validating risk reports
 * Orchestrates the entire calculation flow from DTO to domain analysis
 */
@Service
public class RiskReportIngestionService {
    
    private static final Logger log = LoggerFactory.getLogger(RiskReportIngestionService.class);
    
    private final RiskReportMapper mapper;
    private final RiskCalculationService calculationService;
    
    public RiskReportIngestionService(
        RiskReportMapper mapper,
        RiskCalculationService calculationService
    ) {
        this.mapper = mapper;
        this.calculationService = calculationService;
    }
    
    @Transactional
    public RiskCalculationResult ingestAndCalculate(RiskReportDTO rawReport) {
        log.info("Starting risk report ingestion and calculation");
        
        // Step 1: Validate raw input
        validateRawReport(rawReport);
        log.debug("Raw report validation passed");
        
        // Step 2: Convert DTO → Domain
        IngestedRiskReport ingestedReport = mapper.toDomain(rawReport);
        log.debug("Mapped DTO to domain objects: {} exposures", ingestedReport.getTotalExposures());
        
        // Step 3: Validate domain model
        validateIngestedReport(ingestedReport);
        log.debug("Domain model validation passed");
        
        // Step 4: Calculate risk metrics
        PortfolioAnalysis analysis = calculationService.calculateRisk(
            ingestedReport.batchId(),
            ingestedReport.exposures(),
            ingestedReport.mitigations()
        );
        log.info("Risk calculation completed for batch: {}", ingestedReport.batchId());
        
        // Step 5: Return result
        return new RiskCalculationResult(
            ingestedReport.batchId(),
            ingestedReport.bankInfo(),
            ingestedReport.getTotalExposures(),
            analysis,
            ingestedReport.ingestedAt()
        );
    }
    
    /**
     * Validates raw DTO input
     * Requirements: 9.1, 9.2, 9.3
     */
    private void validateRawReport(RiskReportDTO report) {
        // Requirement 9.1: Bank information is missing
        if (report.bankInfo() == null) {
            throw new InvalidReportException("Bank info is required");
        }
        
        // Requirement 9.2: Exposure portfolio is empty
        if (report.exposures() == null || report.exposures().isEmpty()) {
            throw new InvalidReportException("Loan portfolio cannot be empty");
        }
        
        // Requirement 9.3: Credit risk mitigation data is missing
        if (report.creditRiskMitigation() == null) {
            throw new InvalidReportException("Credit risk mitigation is required");
        }
    }
    
    /**
     * Validates domain model after mapping
     * Requirements: 1.3, 1.4
     */
    private void validateIngestedReport(IngestedRiskReport report) {
        // Requirement 1.3: Verify all exposure IDs are unique
        Set<ExposureId> exposureIds = new HashSet<>();
        for (var exposure : report.exposures()) {
            if (!exposureIds.add(exposure.id())) {
                throw new InvalidReportException("Duplicate exposure IDs detected");
            }
        }
        
        // Requirement 1.4: Verify all mitigation references point to existing exposures
        for (ExposureId mitigationExposureId : report.mitigations().keySet()) {
            if (!exposureIds.contains(mitigationExposureId)) {
                throw new InvalidReportException("Some mitigations reference non-existent exposures");
            }
        }
    }
}
