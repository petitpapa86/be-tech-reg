package com.bcbs239.regtech.reportgeneration.infrastructure.templates;

import com.bcbs239.regtech.reportgeneration.domain.generation.RecommendationSection;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculatedExposure;
import com.bcbs239.regtech.reportgeneration.domain.generation.CalculationResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.HtmlGenerationException;
import com.bcbs239.regtech.reportgeneration.domain.generation.HtmlReportGenerator;
import com.bcbs239.regtech.reportgeneration.domain.generation.QualityResults;
import com.bcbs239.regtech.reportgeneration.domain.generation.ReportMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of HtmlReportGenerator using Thymeleaf template engine.
 * 
 * This service generates professional HTML reports with:
 * - Bank information and regulatory references
 * - Executive summary cards
 * - Interactive charts (sector distribution, top exposures)
 * - Detailed exposure tables
 * - Compliance status indicators
 * - Risk analysis sections
 * - Data quality assessment
 * - Quality recommendations
 * 
 * Requirements: 5.3, 6.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HtmlReportGeneratorImpl implements HtmlReportGenerator {
    
    private final SpringTemplateEngine templateEngine;
    
    /**
     * Generates HTML report from calculation results and metadata.
     * 
     * This method is for backward compatibility and generates a report
     * with only Large Exposures section (no quality data).
     * 
     * @param results the calculation results
     * @param metadata the report metadata
     * @return generated HTML content
     * @throws HtmlGenerationException if generation fails
     */
    @Override
    public String generate(CalculationResults results, ReportMetadata metadata) {
        try {
            log.info("Generating HTML report (calculation only) [batchId:{},bankId:{}]", 
                results.batchId().value(), results.bankId().value());
            
            // Create Thymeleaf context
            Context context = new Context();
            
            // Add calculation results
            context.setVariable("calculationResults", results);
            context.setVariable("metadata", metadata);
            
            // Add chart data
            context.setVariable("sectorChartData", prepareSectorChartData(results));
            context.setVariable("topExposuresChartData", prepareTopExposuresChartData(results));
            
            // Process template
            String html = templateEngine.process("comprehensive-report", context);
            
            log.info("HTML report generated successfully [batchId:{},size:{}]", 
                results.batchId().value(), html.length());
            
            return html;
            
        } catch (Exception e) {
            log.error("Failed to generate HTML report [batchId:{}]", 
                results.batchId().value(), e);
            throw new HtmlGenerationException("Failed to generate HTML report", e);
        }
    }
    
    /**
     * Generates comprehensive HTML report with both calculation and quality data.
     * 
     * The method:
     * 1. Creates Thymeleaf context with all data
     * 2. Prepares chart data for Chart.js
     * 3. Processes template
     * 4. Returns generated HTML string
     * 
     * @param calculationResults the calculation results
     * @param qualityResults the quality results
     * @param recommendations the quality recommendations
     * @param metadata the report metadata
     * @return generated HTML content
     * @throws HtmlGenerationException if generation fails
     */
    @Override
    public String generateComprehensive(
            CalculationResults calculationResults,
            QualityResults qualityResults,
            List<RecommendationSection> recommendations,
            ReportMetadata metadata) {
        try {
            log.info("Generating comprehensive HTML report [batchId:{},bankId:{}]", 
                calculationResults.batchId().value(), calculationResults.bankId().value());
            
            // Create Thymeleaf context
            Context context = new Context();
            
            // Add all data
            context.setVariable("calculationResults", calculationResults);
            context.setVariable("qualityResults", qualityResults);
            context.setVariable("recommendations", recommendations);
            context.setVariable("metadata", metadata);
            
            // Add chart data
            context.setVariable("sectorChartData", prepareSectorChartData(calculationResults));
            context.setVariable("topExposuresChartData", prepareTopExposuresChartData(calculationResults));
            
            // Process template
            String html = templateEngine.process("comprehensive-report", context);
            
            log.info("Comprehensive HTML report generated successfully [batchId:{},size:{}]", 
                calculationResults.batchId().value(), html.length());
            
            return html;
            
        } catch (Exception e) {
            log.error("Failed to generate comprehensive HTML report", e);
            throw new HtmlGenerationException("Failed to generate comprehensive HTML report", e);
        }
    }
    
    /**
     * Prepares sector distribution data for Chart.js donut chart.
     * 
     * Groups exposures by sector and calculates total amounts.
     * 
     * @param results the calculation results
     * @return map with labels and values for chart
     */
    private Map<String, Object> prepareSectorChartData(CalculationResults results) {
        Map<String, Object> chartData = new HashMap<>();
        
        // Group exposures by sector
        Map<String, BigDecimal> sectorTotals = results.exposures().stream()
            .collect(Collectors.groupingBy(
                CalculatedExposure::sectorCode,
                Collectors.reducing(
                    BigDecimal.ZERO,
                    CalculatedExposure::amountEur,
                    BigDecimal::add
                )
            ));
        
        // Sort by amount descending
        List<Map.Entry<String, BigDecimal>> sortedSectors = sectorTotals.entrySet().stream()
            .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        // Extract labels and values
        List<String> labels = sortedSectors.stream()
            .map(entry -> getSectorDisplayName(entry.getKey()))
            .collect(Collectors.toList());
        
        List<BigDecimal> values = sortedSectors.stream()
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        
        chartData.put("labels", labels);
        chartData.put("values", values);
        
        return chartData;
    }
    
    /**
     * Prepares top exposures data for Chart.js horizontal bar chart.
     * 
     * Selects top 10 exposures by amount.
     * 
     * @param results the calculation results
     * @return map with labels and values for chart
     */
    private Map<String, Object> prepareTopExposuresChartData(CalculationResults results) {
        Map<String, Object> chartData = new HashMap<>();
        
        // Get top 10 exposures by amount
        List<CalculatedExposure> topExposures = results.exposures().stream()
            .sorted(Comparator.comparing(CalculatedExposure::amountEur).reversed())
            .limit(10)
            .collect(Collectors.toList());
        
        // Extract labels (counterparty names, truncated if too long)
        List<String> labels = topExposures.stream()
            .map(exposure -> truncateCounterpartyName(exposure.counterpartyName(), 30))
            .collect(Collectors.toList());
        
        // Extract values
        List<BigDecimal> values = topExposures.stream()
            .map(CalculatedExposure::amountEur)
            .collect(Collectors.toList());
        
        chartData.put("labels", labels);
        chartData.put("values", values);
        
        return chartData;
    }
    
    /**
     * Gets display name for sector code.
     * 
     * Maps ESA 2010 sector codes to readable names.
     * 
     * @param sectorCode the sector code (e.g., "S.11")
     * @return display name
     */
    private String getSectorDisplayName(String sectorCode) {
        Map<String, String> sectorNames = Map.ofEntries(
            Map.entry("S.11", "Non-Financial Corporations"),
            Map.entry("S.12", "Financial Corporations"),
            Map.entry("S.121", "Central Bank"),
            Map.entry("S.122", "Deposit-Taking Corporations"),
            Map.entry("S.123", "Money Market Funds"),
            Map.entry("S.124", "Non-MMF Investment Funds"),
            Map.entry("S.125", "Other Financial Intermediaries"),
            Map.entry("S.126", "Financial Auxiliaries"),
            Map.entry("S.127", "Captive Financial Institutions"),
            Map.entry("S.128", "Insurance Corporations"),
            Map.entry("S.129", "Pension Funds"),
            Map.entry("S.13", "General Government"),
            Map.entry("S.14", "Households"),
            Map.entry("S.15", "Non-Profit Institutions")
        );
        
        return sectorNames.getOrDefault(sectorCode, sectorCode);
    }
    
    /**
     * Truncates counterparty name if too long.
     * 
     * @param name the counterparty name
     * @param maxLength maximum length
     * @return truncated name with ellipsis if needed
     */
    private String truncateCounterpartyName(String name, int maxLength) {
        if (name == null) {
            return "Unknown";
        }
        
        if (name.length() <= maxLength) {
            return name;
        }
        
        return name.substring(0, maxLength - 3) + "...";
    }
}
