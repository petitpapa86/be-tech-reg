package com.bcbs239.regtech.reportgeneration.domain.generation;

import com.bcbs239.regtech.core.domain.shared.Result;

import java.util.List;

/**
 * Domain service interface for generating HTML reports
 * 
 * Generates professional HTML reports for Large Exposures with interactive
 * visualizations using Thymeleaf templates, Tailwind CSS, and Chart.js.
 * 
 * This is a domain service interface that will be implemented in the
 * infrastructure layer using Thymeleaf template engine.
 * 
 * Requirements: 5.1, 5.3, 6.1
 */
public interface HtmlReportGenerator {
    
    /**
     * Generate an HTML report from calculation results and metadata
     * 
     * The generated HTML includes:
     * - Header with bank information, reporting date, and regulatory references
     * - Five executive summary cards (Tier 1 Capital, Large Exposures Count, 
     *   Total Amount, Limit Breaches, Sector Concentration)
     * - Interactive donut chart for sector distribution
     * - Horizontal bar chart for top exposures
     * - Sortable table with all exposure details
     * - Compliance status with warning badges for non-compliant exposures
     * - Risk analysis section with concentration risk identification
     * - Footer with generation timestamp and confidentiality notice
     * 
     * @param results the calculation results containing exposure data
     * @param metadata the report metadata (bank ID, reporting date, etc.)
     * @return a Result containing the generated HTML content as a string, or an error
     */
    Result<String> generate(CalculationResults results, ReportMetadata metadata);
    
    /**
     * Generate a comprehensive HTML report with both calculation and quality data
     * 
     * The generated HTML includes all sections from generate() plus:
     * - Data Quality Assessment section with dimension scores
     * - Error distribution visualization
     * - BCBS 239 compliance indicators
     * - Quality recommendations (contextual guidance)
     * 
     * @param calculationResults the calculation results containing exposure data
     * @param qualityResults the quality validation results
     * @param recommendations the quality recommendations
     * @param metadata the report metadata (bank ID, reporting date, etc.)
     * @return a Result containing the generated HTML content as a string, or an error
     */
    Result<String> generateComprehensive(
        CalculationResults calculationResults,
        QualityResults qualityResults,
        List<RecommendationSection> recommendations,
        ReportMetadata metadata
    );
}
