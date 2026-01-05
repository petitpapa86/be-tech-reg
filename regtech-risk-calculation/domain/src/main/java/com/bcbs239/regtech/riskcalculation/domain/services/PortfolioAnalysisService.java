package com.bcbs239.regtech.riskcalculation.domain.services;

import com.bcbs239.regtech.core.domain.shared.ErrorDetail;
import com.bcbs239.regtech.core.domain.shared.ErrorType;
import com.bcbs239.regtech.core.domain.shared.Result;
import com.bcbs239.regtech.riskcalculation.domain.analysis.PortfolioAnalysis;
import com.bcbs239.regtech.riskcalculation.domain.classification.ClassifiedExposure;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Domain service for orchestrating portfolio analysis.
 * 
 * This service encapsulates the portfolio analysis business logic,
 * providing a clean interface for creating portfolio analyses from
 * classified exposures.
 */
@Slf4j
public class PortfolioAnalysisService {

    /**
     * Analyze a portfolio from classified exposures.
     *
     * @param batchId             The batch identifier
     * @param classifiedExposures The classified exposures to analyze
     * @param correlationId       The correlation identifier for tracing
     * @return Result containing the portfolio analysis
     */
    public Result<PortfolioAnalysis> analyzePortfolio(
            String batchId,
            List<ClassifiedExposure> classifiedExposures,
            String correlationId) {

        try {
            log.info("Starting portfolio analysis for batch: {} with {} exposures",
                    batchId, classifiedExposures.size());

            if (classifiedExposures.isEmpty()) {
                return Result.failure(ErrorDetail.of(
                    "PORTFOLIO_NO_EXPOSURES",
                    ErrorType.VALIDATION_ERROR,
                    "Cannot analyze portfolio with no exposures",
                    "validation.portfolio_no_exposures"
                ));
            }

            // Delegate to the aggregate's factory method
            PortfolioAnalysis analysis = PortfolioAnalysis.analyze(batchId, classifiedExposures, correlationId);

            log.info("Portfolio analysis completed - Geographic HHI: {}, Sector HHI: {}",
                    analysis.getGeographicHHI().value(), analysis.getSectorHHI().value());

            return Result.success(analysis);

        } catch (Exception e) {
            log.error("Portfolio analysis failed for batch: {}", batchId, e);
            return Result.failure(ErrorDetail.of(
                "PORTFOLIO_ANALYSIS_FAILED",
                ErrorType.BUSINESS_RULE_ERROR,
                "Portfolio analysis failed: " + e.getMessage(),
                "error.portfolio_analysis_failed"
            ));
        }
    }
}