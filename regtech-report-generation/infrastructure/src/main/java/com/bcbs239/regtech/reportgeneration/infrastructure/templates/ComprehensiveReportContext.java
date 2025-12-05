package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.RecommendationSection;
import com.bcbs239.regtech.reportgeneration.domain.generation.ReportMetadata;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Context object for comprehensive report template rendering.
 * 
 * Contains all data needed for generating a comprehensive HTML report:
 * - Calculation results (Large Exposures analysis)
 * - Quality results (Data Quality assessment)
 * - Quality recommendations (contextual guidance)
 * - Report metadata (bank info, dates, etc.)
 * 
 * This context is passed to the Thymeleaf template engine for rendering.
 */
@Getter
@Builder
public class ComprehensiveReportContext {
    
    /**
     * Calculation results containing Large Exposures data
     */
    private final CalculationResults calculationResults;
    
    /**
     * Quality results containing Data Quality assessment
     */
    private final QualityResults qualityResults;
    
    /**
     * Quality recommendations generated based on quality results
     */
    private final List<RecommendationSection> recommendations;
    
    /**
     * Report metadata (bank ID, reporting date, generation timestamp)
     */
    private final ReportMetadata metadata;
}
