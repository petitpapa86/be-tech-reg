package com.bcbs239.regtech.reportgeneration.domain.generation;

/**
 * Domain service interface for generating HTML reports
 * 
 * Generates professional HTML reports for Large Exposures with interactive
 * visualizations using Thymeleaf templates, Tailwind CSS, and Chart.js.
 * 
 * This is a domain service interface that will be implemented in the
 * infrastructure layer using Thymeleaf template engine.
 * 
 * Requirements: 5.1
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
     * @return the generated HTML content as a string
     * @throws HtmlGenerationException if HTML generation fails
     */
    String generate(CalculationResults results, ReportMetadata metadata);
}
